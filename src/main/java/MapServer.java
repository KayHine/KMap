import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;

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
    private static QuadTree tree;
    private static LinkedList<Long> route;

    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        long startTime = System.nanoTime();
        g = new GraphDB(OSM_DB_PATH);
        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1000000;
        System.out.println("Graph Build Duration: " + duration + " ms");

        tree = new QuadTree();

        startTime = System.nanoTime();
        initializeTree(tree);
        endTime = System.nanoTime();
        duration = (endTime - startTime) / 1000000;
        System.out.println("Tree Build Duration: " + duration + " ms");

        route = new LinkedList<>();
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
            route = findAndSetRoute(params);
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
        LinkedList<QTreeNode> rastersNodes = new LinkedList<>();
        BufferedImage rasterImage;
        boolean query_success = true;

        double dpp = (params.get("lrlon") - params.get("ullon")) / params.get("w");

        double winUllon = params.get("ullon");
        double winUllat = params.get("ullat");
        double winLrlon = params.get("lrlon");
        double winLrlat = params.get("lrlat");
        double[] viewBox = {winUllon, winUllat, winLrlon, winLrlat};

        rastersNodes = (LinkedList<QTreeNode>) tree.gatherNodesInRange(dpp, viewBox);
        // Get rasterNodes in reverse order (largest -> smallest) latitude value
        // so we can render the image from the top down
        Collections.sort(rastersNodes, Collections.reverseOrder());
        rasterImage = buildRasterImage(rastersNodes, query_success);

        try {
            ImageIO.write(rasterImage, "png", os);
        } catch (IOException e) {
            query_success = false;
            e.printStackTrace();
        }

        // Build the rasteredImageParams map
        rasteredImageParams.put("raster_ul_lon", rastersNodes.getFirst().ullon);
        rasteredImageParams.put("raster_ul_lat", rastersNodes.getFirst().ullat);
        rasteredImageParams.put("raster_lr_lon", rastersNodes.getLast().lrlon);
        rasteredImageParams.put("raster_lr_lat", rastersNodes.getLast().lrlat);
        rasteredImageParams.put("raster_width", rasterImage.getWidth());
        rasteredImageParams.put("raster_height", rasterImage.getHeight());
        rasteredImageParams.put("depth", rastersNodes.getFirst().imageName.length());
        rasteredImageParams.put("query_success", query_success);

        return rasteredImageParams;
    }

    public static BufferedImage buildRasterImage(Iterable<QTreeNode> rasterNodes, boolean query_success) {
        BufferedImage rasteredImage;
        HashSet<Double> latitudes = new HashSet<>();

        // Find the number of unique latitudes in the set of rasterNodes to determine the height of the image
        for (QTreeNode node : rasterNodes) {
            latitudes.add(node.ullat);
        }

        int x = 0;
        int y = 0;
        int height = latitudes.size();
        int width = ((LinkedList<QTreeNode>) rasterNodes).size() / height;

        rasteredImage = new BufferedImage(width * TILE_SIZE, height * TILE_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = rasteredImage.getGraphics();

        for (QTreeNode node : rasterNodes) {
            BufferedImage tile = null;
            if (x >= rasteredImage.getWidth()) {
                x = 0;
                y += TILE_SIZE;
            }

            try {
                tile = ImageIO.read(new File(IMG_ROOT + node.imageName + ".png"));
            } catch (IOException e) {
                query_success = false;
                e.printStackTrace();
            }

            graphics.drawImage(tile, x, y, null);
            x += TILE_SIZE;
        }

        /* ------------------------*/
        // Build route if it exists
        if (!route.isEmpty() && route != null) {
            Graphics2D graphics2D = (Graphics2D) graphics;
            BasicStroke line = new BasicStroke(ROUTE_STROKE_WIDTH_PX, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            graphics2D.setStroke(line);
            graphics2D.setPaint(ROUTE_STROKE_COLOR);
            // Get raster image bounds
            double minLat = ((LinkedList<QTreeNode>) rasterNodes).getLast().lrlat;
            double maxLat = ((LinkedList<QTreeNode>) rasterNodes).getFirst().ullat;
            double minLon = ((LinkedList<QTreeNode>) rasterNodes).getFirst().ullon;
            double maxLon = ((LinkedList<QTreeNode>) rasterNodes).getLast().lrlon;
            // Calculate scale pixel/coordinate
            double lonScale = pixelPerCoordinate(minLon, maxLon, minLat, maxLat, rasteredImage, "lon");
            double latScale = pixelPerCoordinate(minLon, maxLon, minLat, maxLat, rasteredImage, "lat");
            for (int i = 0; i < route.size() - 1; i++) {
                Node point1 = g.getNodeByID(route.get(i));
                Node point2 = g.getNodeByID(route.get(i + 1));
                int point1_x = getPixelPositionOffset(point1, minLat, latScale, minLon, lonScale, "lon");
                int point1_y = getPixelPositionOffset(point1, minLat, latScale, minLon, lonScale, "lat");
                int point2_x = getPixelPositionOffset(point2, minLat, latScale, minLon, lonScale, "lat");
                int point2_y = getPixelPositionOffset(point2, minLat, latScale, minLon, lonScale, "lat");
                graphics2D.drawLine(point1_x, point1_y, point2_x, point2_y);
            }
        }
        /* ------------------------*/

        return rasteredImage;
    }

    public static int getPixelPositionOffset(Node point, double minLat, double latScale, double minLon,
                                       double lonScale, String coord) {
        int pos = 0;
        if (coord == "lat") {
            pos = (int) ((point.latitude - minLat) * latScale);
        }
        else if (coord == "lon") {
            pos = (int) ((point.longitude - minLon) * lonScale);
        }
        return pos;
    }

    public static double pixelPerCoordinate(double minLon, double maxLon, double minLat, double maxLat,
                                            BufferedImage rasteredImage, String coord) {
        double scale = 0;
        if (coord == "lat") {

            scale = rasteredImage.getHeight() / (maxLat - minLat);
        }
        else if (coord == "lon") {

            scale = rasteredImage.getWidth() / (maxLon - minLon);
        }

        return scale;
    }


    /**
     * Initializing function that createst the entire QuadTree with all images
     * @param tree - takes the private static QuadTree variable in the MapServer class
     */
    public static void initializeTree(QuadTree tree) {
        // Add the initial root node
        tree.addQTreeNode(ROOT_ULLON, ROOT_ULLAT, ROOT_LRLON, ROOT_LRLAT, "root");
        double[] root_coord = {ROOT_ULLON, ROOT_ULLAT, ROOT_LRLON, ROOT_LRLAT};
        treeBuilder(tree, root_coord, "root");
    }

    /**
     * Helper function that recursively builds the entire QuadTree.
     * 1. Check if the png exists in /img/ folder
     * 2. Split incoming coordinates[] of node into four quadrants
     * 3. Build the image file name based on if the previous name is root or not
     * 4. Build the entire level of each quadrant
     * 5. Recursively build each quadrant
     * @param tree - takes the private static QuadTree variable in the MapServer class
     * @param coordinates - the coordinates of the root node
     * @param filename - name of the file that we'll ultimately use to load the png file
     */
    public static void treeBuilder(QuadTree tree, double[] coordinates, String filename) {

        if (!isValidFile(filename))
            return;

        String imgName1, imgName2, imgName3, imgName4;
        imgName1 = imgName2 = imgName3 = imgName4 = filename;
        double lonMid = (coordinates[0] + coordinates[2]) / 2;
        double latMid = (coordinates[1] + coordinates[3]) / 2;

        // Split the current node into four quadrants
        double[] northWest = {coordinates[0], coordinates[1], lonMid, latMid};
        double[] northEast = {lonMid, coordinates[1], coordinates[2], latMid};
        double[] southWest = {coordinates[0], latMid, lonMid, coordinates[3]};
        double[] southEast = {lonMid, latMid, coordinates[2], coordinates[3]};


        if (filename == "root") {
            imgName1 = "1";
            tree.addQTreeNode(northWest[0], northWest[1], northWest[2], northWest[3], imgName1);
            treeBuilder(tree, northWest, imgName1);

            imgName2 = "2";
            tree.addQTreeNode(northEast[0], northEast[1], northEast[2], northEast[3], imgName2);
            treeBuilder(tree, northEast, imgName2);

            imgName3 = "3";
            tree.addQTreeNode(southWest[0], southWest[1], southWest[2], southWest[3], imgName3);
            treeBuilder(tree, southWest, imgName3);

            imgName4 = "4";
            tree.addQTreeNode(southEast[0], southEast[1], southEast[2], southEast[3], imgName4);
            treeBuilder(tree, southEast, imgName4);
        }
        else {
            imgName1 = imgName1 + "1";
            if (isValidFile(imgName1)) {
                tree.addQTreeNode(northWest[0], northWest[1], northWest[2], northWest[3], imgName1);
            }

            imgName2 = filename;
            imgName2 = imgName2 + "2";
            if (isValidFile(imgName2)) {
                tree.addQTreeNode(northEast[0], northEast[1], northEast[2], northEast[3], imgName2);
            }

            imgName3 = filename;
            imgName3 = imgName3 + "3";
            if (isValidFile(imgName3)) {
                tree.addQTreeNode(southWest[0], southWest[1], southWest[2], southWest[3], imgName3);
            }

            imgName4 = filename;
            imgName4 = imgName4 + "4";
            if (isValidFile(imgName4)) {
                tree.addQTreeNode(southEast[0], southEast[1], southEast[2], southEast[3], imgName4);
            }

            treeBuilder(tree, northWest, imgName1);
            treeBuilder(tree, northEast, imgName2);
            treeBuilder(tree, southWest, imgName3);
            treeBuilder(tree, southEast, imgName4);
        }
    }

    public static boolean isValidFile(String filename) {
        File image = new File(IMG_ROOT + filename + ".png");
        return image.exists();
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
        LinkedList<Long> shortestRoute = new LinkedList<>();
        double start_lon = params.get("start_lon");
        double start_lat = params.get("start_lat");
        double end_lon = params.get("end_lon");
        double end_lat = params.get("end_lat");

        Node startNode = g.getNearestNode(start_lon, start_lat);
        Node endNode = g.getNearestNode(end_lon, end_lat);

        shortestRoute = getShortestPath(startNode, endNode);

        return shortestRoute;
    }

    /**
     * Use A* search algorithm to get the shortest route from the start node to end node
     * Prority associated with a node: f(n) = g(n) + h(n)
     * g(n): shortest known path distance from s to n
     * h(n): Euclidean distance from n to t. Distance between two points = sqrt((x1 - x0)^2 + (y1 - y0)^2))
     * @param start
     * @param end
     * @return
     */
    public static LinkedList<Long> getShortestPath(Node start, Node end) {
        // Using cool Lambda function to create anonymous Comparator function class
        // to sort Nodes based on calculated heuristic
        final Queue<Node> openQueue = new PriorityQueue<>(11, (node1, node2) -> {
            if (node1.getHeuristic() < node2.getHeuristic())
                return -1;
            else if (node1.getHeuristic() > node2.getHeuristic())
                return 1;
            return 0;
        });

        start.setDist(0);
        start.setHeuristic(end);
        openQueue.add(start);

        final LinkedList<Long> shortestPath = new LinkedList<>();
        final HashSet<Node> closedList = new HashSet<>();

        while (!openQueue.isEmpty()) {
            final Node currentNode = openQueue.poll();

            if (currentNode.equals(end)) {
                shortestPath.add(currentNode.id);
                return shortestPath;
            }

            closedList.add(currentNode);

            for (Node neighbor : g.getNeighbors(currentNode)) {
                if (closedList.contains(neighbor)) continue;

                double distance = neighbor.distanceBetweenNodes(currentNode);
                double tentativeDist = distance + currentNode.getDist();

                if (tentativeDist < neighbor.getDist()) {
                    neighbor.setDist(tentativeDist);
                    neighbor.setHeuristic(end);

                    shortestPath.add(neighbor.id);
                    if (!openQueue.contains(neighbor)) {
                        openQueue.add(neighbor);
                    }
                }
            }
        }

        return shortestPath;
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
        return g.getAutoCompleteSuggestions(prefix);
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
