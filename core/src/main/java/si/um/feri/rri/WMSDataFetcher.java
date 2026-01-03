package si.um.feri.rri;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class WMSDataFetcher {

    // Correct layer names from WMS GetCapabilities
    private static final String LAYER_KINDERGARTENS = "medob_vrtci";
    private static final String LAYER_PLAYGROUNDS = "medob_otr_igrala";
    private static final String LAYER_TRAIN = "int_mob_vlakec_jurcek";

    private JsonReader jsonReader;

    public WMSDataFetcher() {
        jsonReader = new JsonReader();
    }

    public interface DataCallback {
        void onSuccess(Array<LocationData> data);
        void onFailure(String error);
    }

    public static class LocationData {
        public String name;
        public double latitude;
        public double longitude;
        public String type;
        public JsonValue properties;

        public LocationData() {
        }

        public LocationData(String name, double lat, double lon, String type) {
            this.name = name;
            this.latitude = lat;
            this.longitude = lon;
            this.type = type;
        }
    }

    public void fetchKindergartens(final DataCallback callback) {
        fetchWFSData(LAYER_KINDERGARTENS, "kindergarten", callback);
    }

    public void fetchPlaygrounds(final DataCallback callback) {
        fetchWFSData(LAYER_PLAYGROUNDS, "playground", callback);
    }

    public void fetchTrainRoute(final DataCallback callback) {
        fetchWFSData(LAYER_TRAIN, "train", callback);
    }

    private void fetchWFSData(final String layerName, final String type, final DataCallback callback) {
        // Build WFS GetFeature request
        String wfsUrl = "https://prostor.maribor.si/ows/public/wfs" +
            "?service=WFS" +
            "&version=1.1.0" +
            "&request=GetFeature" +
            "&typeName=" + layerName +
            "&outputFormat=application/json" +
            "&srsName=EPSG:4326"; // Request WGS84 coordinates

        Gdx.app.log("WMSDataFetcher", "Fetching " + type + " from layer: " + layerName);

        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder. newRequest()
            .method(Net.HttpMethods.GET)
            .url(wfsUrl)
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0")
            .build();

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                try {
                    int statusCode = httpResponse.getStatus().getStatusCode();
                    String responseString = httpResponse.getResultAsString();

                    if (statusCode != 200) {
                        Gdx.app.error("WMSDataFetcher", "HTTP " + statusCode);
                        callback.onFailure("HTTP " + statusCode);
                        return;
                    }

                    // Check if response is XML error
                    String trimmed = responseString.trim();
                    if (trimmed.startsWith("<?xml") || trimmed.startsWith("<ows:")) {
                        Gdx. app.error("WMSDataFetcher", "Server returned XML error");
                        callback.onFailure("Server error");
                        return;
                    }

                    // Remove the problematic bbox fields that LibGDX JsonReader can't handle
                    responseString = removeBboxFields(responseString);

                    // Parse GeoJSON
                    Array<LocationData> locations = parseGeoJSON(responseString, type);

                    Gdx.app.log("WMSDataFetcher", "Successfully parsed " + locations.size + " " + type);
                    callback.onSuccess(locations);

                } catch (Exception e) {
                    Gdx.app.error("WMSDataFetcher", "Error:  " + e.getMessage());
                    e.printStackTrace();
                    callback. onFailure("Error:  " + e.getMessage());
                }
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error("WMSDataFetcher", "Network error: " + t.getMessage());
                callback.onFailure("Network error: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                callback.onFailure("Request cancelled");
            }
        });
    }

    /**
     * Remove bbox fields that cause LibGDX JsonReader to fail
     * Pattern: "bbox":[15.7566,46.504,15.7566,46.504]
     */
    private String removeBboxFields(String json) {
        // Use regex to remove bbox fields
        // Pattern: "bbox":[numbers]
        json = json.replaceAll(",\"bbox\":\\[[^\\]]+\\]", "");

        // Also remove featureid which might cause issues
        json = json.replaceAll(",\"featureid\":\\d+", "");

        return json;
    }

    private Array<LocationData> parseGeoJSON(String json, String type) {
        Array<LocationData> locations = new Array<>();

        try {
            JsonValue root = jsonReader.parse(json);
            JsonValue features = root.get("features");

            if (features == null) {
                Gdx. app.error("WMSDataFetcher", "No features in response");
                return locations;
            }

            int featureCount = 0;
            for (JsonValue feature : features) {
                featureCount++;
                try {
                    JsonValue geometry = feature.get("geometry");
                    JsonValue properties = feature.get("properties");

                    if (geometry == null) {
                        continue;
                    }

                    LocationData data = new LocationData();
                    data. type = type;
                    data. properties = properties;

                    // Extract name from properties
                    if (properties != null) {
                        String[] nameFields = {
                            "ime_vrtca", "enota_vrtca", "naslov_enote",  // For kindergartens
                            "lokacija", "vrsta_igral",  // For playgrounds
                            "postaja", "naziv",  // For train stops
                            "ime", "IME", "Ime",
                            "naziv", "NAZIV", "Naziv",
                            "name", "NAME", "Name",
                            "naslov", "NASLOV", "Naslov"
                        };

                        data.name = null;
                        for (String field :  nameFields) {
                            if (properties.has(field)) {
                                String value = properties.getString(field, null);
                                if (value != null && !value.isEmpty() && !value.equalsIgnoreCase("null") && !value.equalsIgnoreCase("ni podatka")) {
                                    data. name = value;
                                    break;
                                }
                            }
                        }

                        if (data. name == null || data.name. isEmpty()) {
                            data. name = type + " " + (locations.size + 1);
                        }
                    } else {
                        data.name = type + " " + (locations. size + 1);
                    }

                    // Extract coordinates
                    String geometryType = geometry.getString("type", "");
                    JsonValue coordinates = geometry.get("coordinates");

                    if (coordinates == null) {
                        continue;
                    }

                    boolean extracted = extractCoordinates(data, geometryType, coordinates);

                    if (extracted) {
                        locations.add(data);
                        if (locations. size <= 5) { // Log first 5
                            Gdx.app. log("WMSDataFetcher", "  Added:  " + data.name + " at [" + data.latitude + ", " + data.longitude + "]");
                        }
                    }

                } catch (Exception e) {
                    Gdx.app.log("WMSDataFetcher", "Error parsing feature " + featureCount + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            Gdx.app.error("WMSDataFetcher", "Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
        }

        return locations;
    }

    private boolean extractCoordinates(LocationData data, String geometryType, JsonValue coordinates) {
        try {
            switch (geometryType) {
                case "Point":
                    data.longitude = coordinates.getDouble(0);
                    data.latitude = coordinates.getDouble(1);
                    return true;

                case "LineString":
                    JsonValue firstPoint = coordinates.get(0);
                    if (firstPoint != null) {
                        data.longitude = firstPoint.getDouble(0);
                        data.latitude = firstPoint.getDouble(1);
                        return true;
                    }
                    break;

                case "MultiLineString":
                    JsonValue firstLine = coordinates.get(0);
                    if (firstLine != null) {
                        JsonValue point = firstLine.get(0);
                        if (point != null) {
                            data.longitude = point.getDouble(0);
                            data.latitude = point.getDouble(1);
                            return true;
                        }
                    }
                    break;

                case "Polygon":
                    JsonValue ring = coordinates.get(0);
                    if (ring != null) {
                        JsonValue point = ring.get(0);
                        if (point != null) {
                            data.longitude = point.getDouble(0);
                            data.latitude = point. getDouble(1);
                            return true;
                        }
                    }
                    break;

                case "MultiPolygon":
                    JsonValue polygon = coordinates.get(0);
                    if (polygon != null) {
                        JsonValue pRing = polygon.get(0);
                        if (pRing != null) {
                            JsonValue point = pRing.get(0);
                            if (point != null) {
                                data.longitude = point. getDouble(0);
                                data.latitude = point.getDouble(1);
                                return true;
                            }
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            Gdx.app.log("WMSDataFetcher", "Error extracting coordinates: " + e.getMessage());
        }
        return false;
    }
}
