import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* Maven is used to pull in these dependencies. */
import com.google.gson.Gson;

import javax.imageio.ImageIO;

import static spark.Spark.*;

/**
 * This MapServer class is the entry point for running the JavaSpark web server for the BearMaps
 * application project, receiving API calls, handling the API call processing, and generating
 * requested images and routes.
 * @author Alan Yao
 */
public class MapServer {
    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /** Each tile is 256x256 pixels. */
    public static final int TILE_SIZE = 256;
    /** HTTP failed response. */
    private static final int HALT_RESPONSE = 403;
    /** Route stroke information: typically roads are not more than 5px wide. */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /** Route stroke information: Cyan with half transparency. */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);
    /** The tile images are in the IMG_ROOT folder. */
    private static final String IMG_ROOT = "img/";
    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
        "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    private static final String[] REQUIRED_ROUTE_REQUEST_PARAMS = {"start_lat", "start_lon",
        "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB g;

    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        g = new GraphDB(OSM_DB_PATH);
    }

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });

        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAMS);
            /* The png image is written to the ByteArrayOutputStream */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            /* getMapRaster() does almost all the work for this API call */
            Map<String, Object> rasteredImgParams = getMapRaster(params, os);
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the routing endpoint for HTTP GET requests. */
        get("/route", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_ROUTE_REQUEST_PARAMS);
            LinkedList<Long> route = findAndSetRoute(params);
            return !route.isEmpty();
        });

        /* Define the API endpoint for clearing the current route. */
        get("/clear_route", (req, res) -> {
            clearRoute();
            return true;
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Validate & return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     * @param req HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (!reqParams.contains(param)) {
                halt(HALT_RESPONSE, "Request failed - parameters missing.");
            } else {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }


    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     *     The rastered photo must have the following properties:
     *     <ul>
     *         <li>Has dimensions of at least w by h, where w and h are the user viewport width
     *         and height.</li>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *         <li>If a current route exists, lines of width ROUTE_STROKE_WIDTH_PX and of color
     *         ROUTE_STROKE_COLOR are drawn between all nodes on the route in the rastered photo.
     *         </li>
     *     </ul>
     *     Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query bounding box and
     *               the user viewport width and height.
     * @param os     An OutputStream that the resulting png image should be written to.
     * @return A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Double, the width of the rastered image <br>
     * "raster_height" -> Double, the height of the rastered image <br>
     * "depth"         -> Double, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     */
    public static Map<String, Object> getMapRaster(Map<String, Double> params, OutputStream os) {
        HashMap<String, Object> rasteredImageParams = new HashMap<>();
        LinkedList<QTreeNode> rasters = new LinkedList<>();
        QuadTree tree = new QuadTree();
        int treeHeight = 0;
        double dpp = (params.get("lrlon") - params.get("ullon")) / params.get("w");

        cleanParams(params);

        double winUllon = params.get("ullon");
        double winUllat = params.get("ullat");
        double winLrlon = params.get("lrlon");
        double winLrlat = params.get("lrlat");

        double root_dpp = (ROOT_LRLON - ROOT_ULLON) / TILE_SIZE;

        BufferedImage root_pic = null;
        root_pic = makeImage("root");

        tree.put(ROOT_ULLON, ROOT_ULLAT, ROOT_LRLON, ROOT_LRLAT, "root", root_pic);

        double[] root_coord = {ROOT_ULLON, ROOT_ULLAT, ROOT_LRLON, ROOT_LRLAT};
        double[] target_coord = {winUllon, winUllat, winLrlon, winLrlat};

        // Recursively build the tree based on the viewing window
        buildTree(root_coord, target_coord, dpp, root_dpp, "root", tree);
        treeHeight = tree.getHeight();
        tree.getLowestLevel(treeHeight, rasters);

        // Build the actual image now


        return rasteredImageParams;
    }

    public static void buildTree(double[] node,
                                 double[] target,
                                 double dpp,
                                 double node_dpp,
                                 String imgName,
                                 QuadTree tree) {
        // Base case
        if (node_dpp < dpp) {
            return;
        }

        double node_ullon, node_ullat, node_lrlon, node_lrlat;

        node_ullon = node[0];
        node_ullat = node[1];
        node_lrlon = node[2];
        node_lrlat = node[3];

        double avg_lon = (node_ullon + node_lrlon) / 2;
        double avg_lat = (node_ullat + node_lrlat) / 2;

        double[] topLeftChild = {node_ullon, node_ullat, avg_lon, avg_lat + Math.pow(10, -9)};
        double[] topRightChild = {avg_lon + Math.pow(10, -9), node_ullat, node_lrlon, avg_lat + + Math.pow(10, -9)};
        double[] bottomLeftChild = {node_ullon, avg_lat, avg_lon, node_lrlat};
        double[] bottomRightChild = {avg_lon + Math.pow(10, -9), avg_lat, node_lrlon, node_lrlat};

        node_dpp = (avg_lon - node_ullon) / TILE_SIZE;
        String filename = imgName;

        if (isInBound(topLeftChild, target)) {
            if (imgName == "root") {
                filename = "1";
            } else {
                filename = filename + "1";
            }
            BufferedImage newImage = makeImage(filename);
            tree.put(topLeftChild[0], topLeftChild[1], topLeftChild[2],
                    topLeftChild[3], filename, newImage);
            buildTree(topLeftChild, target, dpp, node_dpp, filename, tree);
        }

        filename = imgName;
        if (isInBound(topRightChild, target)) {
            if (imgName == "root") {
                filename = "2";
            } else {
                filename = filename + "2";
            }
            BufferedImage newImage = makeImage(filename);
            tree.put(topRightChild[0], topRightChild[1], topRightChild[2],
                    topRightChild[3], filename, newImage);
            buildTree(topRightChild, target, dpp, node_dpp, filename, tree);
        }

        filename = imgName;
        if (isInBound(bottomLeftChild, target)) {
            if (imgName == "root") {
                filename = "3";
            } else {
                filename = filename + "3";
            }
            BufferedImage newImage = makeImage(filename);
            tree.put(bottomLeftChild[0], bottomLeftChild[1], bottomLeftChild[2],
                    bottomLeftChild[3], filename, newImage);
            buildTree(bottomLeftChild, target, dpp, node_dpp, filename, tree);
        }

        filename = imgName;
        if (isInBound(bottomRightChild, target)) {
            if (imgName == "root") {
                filename = "4";
            } else {
                filename = filename + "4";
            }
            BufferedImage newImage = makeImage(filename);
            tree.put(bottomRightChild[0], bottomRightChild[1], bottomRightChild[2],
                    bottomRightChild[3], filename, newImage);
            buildTree(bottomRightChild, target, dpp, node_dpp, filename, tree);
        }
    }

    public static boolean isInBound(double[] node, double[] target) {
        // If one rectangle is on the left side of the other
        if (node[0] > target[2] || target[0] > node[2]) {
            return false;
        }

        // If one rectangle is above the other
        if (node[1] < target[3] || target[1] < node[3]) {
            return false;
        }

        return true;
    }

    public static BufferedImage makeImage(String imgName) {
        BufferedImage img = null;
        try {
            img = ImageIO.read(new File(IMG_ROOT + imgName + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    public static void cleanParams(Map<String, Double> params) {
        double winUllon = params.get("ullon");
        double winUllat = params.get("ullat");
        double winLrlon = params.get("lrlon");
        double winLrlat = params.get("lrlat");
        if (winUllon < ROOT_ULLON) {
            winUllon = ROOT_ULLON;
        }
        if (winUllat > ROOT_ULLAT) {
            winUllat = ROOT_ULLAT;
        }
        if (winLrlon > ROOT_LRLON) {
            winLrlon = ROOT_LRLON;
        }
        if (winLrlat < ROOT_LRLAT) {
            winLrlat = ROOT_LRLAT;
        }
        params.put("ullon", winUllon);
        params.put("ullat", winUllat);
        params.put("lrlon", winLrlon);
        params.put("lrlat", winLrlat);
    }

    /**
     * Searches for the shortest route satisfying the input request parameters, sets it to be the
     * current route, and returns a <code>LinkedList</code> of the route's node ids for testing
     * purposes. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean between two points (lon1, lat1) and
     * (lon2, lat2).
     * @param params from the API call described in REQUIRED_ROUTE_REQUEST_PARAMS
     * @return A LinkedList of node ids from the start of the route to the end.
     */
    public static LinkedList<Long> findAndSetRoute(Map<String, Double> params) {
        return new LinkedList<>();
    }

    /**
     * Clear the current found route, if it exists.
     */
    public static void clearRoute() {
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        return new LinkedList<>();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public static List<Map<String, Object>> getLocations(String locationName) {
        return new LinkedList<>();
    }
}
