/*
 * Copyright 2016 Esri.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
/*
 * Copyright 2016 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.esri.routing;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.FeatureCollectionTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureCollectionLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.DrawStatus;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol.Style;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol.HorizontalAlignment;
import com.esri.arcgisruntime.symbology.TextSymbol.VerticalAlignment;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class DeliverTillYouDrop extends Application {

  private MapView mapView;
  private RouteTask routeTask;
  private RouteParameters routeParameters;
  private ListView<String> directionsList = new ListView<>();

  private Graphic routeGraphic;
  private GraphicsOverlay routeGraphicsOverlay = new GraphicsOverlay();

  private final SpatialReference ESPG_3857 = SpatialReference.create(102100);
  private static final int BLACK = 0xff000000;

  private static final String ROUTE_TASK_SANDIEGO =
      "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route";

  @Override
  public void start(Stage stage) throws Exception {

    try {
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

      stage.setTitle("DelieverTillYouDrop");
      stage.setWidth(1000);
      stage.setHeight(700);
      stage.setScene(scene);
      stage.show();

      // create a control panel
      HBox hboxControl = new HBox(6);
      hboxControl.setMaxSize(500, 30);
      hboxControl.getStyleClass().add("panel-region");
      VBox selectRouteControl = new VBox(6);
      selectRouteControl.setMaxSize(200, 30);
      selectRouteControl.getStyleClass().add("panel-region");
      Label routesLabel = new Label("Select the Routes to show:");
      routesLabel.getStyleClass().add("panel-label");
      // create list of routes
      ObservableList<String> routesList = FXCollections.observableArrayList();
      routesList.add("All Routes");
      routesList.add("Route 1");
      routesList.add("Route 2");
      routesList.add("Route 3");
      routesList.add("Route 4");
      routesList.add("Route 5");
      // create combo box
      ComboBox<String> comboBox = new ComboBox<>(routesList);
      comboBox.setMaxWidth(Double.MAX_VALUE);
      comboBox.setDisable(false);
      selectRouteControl.getChildren().addAll(routesLabel, comboBox);
      // create buttons for user interaction
      Label routeButtons = new Label("Routes interaction:");
      routeButtons.getStyleClass().add("panel-label");
      Button addRouteButton = new Button("Add route");
      addRouteButton.setMaxWidth(Double.MAX_VALUE);
      addRouteButton.setDisable(false);
      Button deleteRouteButton = new Button("Delete route");
      deleteRouteButton.setMaxWidth(Double.MAX_VALUE);
      deleteRouteButton.setDisable(false);
      VBox routeControl = new VBox(6);
      routeControl.setMaxSize(150, 30);
      routeControl.getStyleClass().add("panel-region");
      routeControl.getChildren().addAll(routeButtons, addRouteButton, deleteRouteButton);
      Label barriers = new Label("Barriers interaction:");
      barriers.getStyleClass().add("panel-label");
      Button addBarrierButton = new Button("Add Barrier");
      addBarrierButton.setMaxWidth(Double.MAX_VALUE);
      addBarrierButton.setDisable(false);
      Button deleteBarrierButton = new Button("Delete Barrier");
      deleteBarrierButton.setMaxWidth(Double.MAX_VALUE);
      deleteBarrierButton.setDisable(false);
      VBox barrierControl = new VBox(6);
      barrierControl.setMaxSize(150, 30);
      barrierControl.getStyleClass().add("panel-region");
      barrierControl.getChildren().addAll(barriers, addBarrierButton, deleteBarrierButton);
      Label find = new Label("Find/Clear routes:");
      find.getStyleClass().add("panel-label");
      Button findButton = new Button("Find route");
      findButton.setMaxWidth(Double.MAX_VALUE);
      findButton.setDisable(true);
      Button resetButton = new Button("Reset");
      resetButton.setMaxWidth(Double.MAX_VALUE);
      resetButton.setDisable(true);
      VBox findRoutesControl = new VBox(6);
      findRoutesControl.setMaxSize(150, 30);
      findRoutesControl.getStyleClass().add("panel-region");
      findRoutesControl.getChildren().addAll(find, findButton, resetButton);

      // TODO: clear the routes from the map
      // resetButton
      // add buttons and direction list and label to the control panel
      hboxControl.getChildren().addAll(selectRouteControl, routeControl, barrierControl, findRoutesControl);

      ArcGISMap map = new ArcGISMap(Basemap.createStreets());

      mapView = new MapView();
      mapView.setMap(map);

      mapView.addDrawStatusChangedListener(e -> {
        if (e.getDrawStatus() == DrawStatus.COMPLETED) {
          findButton.setDisable(false);
        }
      });

      mapView.setViewpointCenterAsync(new Point(-1.3042962793075608E7, 3857768.9280015198, ESPG_3857), 10000);

      mapView.getGraphicsOverlays().add(routeGraphicsOverlay);

      try {
        routeTask = new RouteTask(ROUTE_TASK_SANDIEGO);

        routeTask.loadAsync();
        routeTask.addDoneLoadingListener(() -> {

          try {

            routeParameters = routeTask.createDefaultParametersAsync().get();
            routeParameters.setOutputSpatialReference(ESPG_3857);

            routeParameters.setReturnStops(true);
            routeParameters.setReturnDirections(true);

            JsonObject validFCJSON = getTestAssetJson(getClass().getResource("/deliveries.txt").getPath());
            FeatureCollection featureCollection = FeatureCollection.fromJson(validFCJSON.toString());
            List<FeatureCollectionTable> featureTables = featureCollection.getTables();
            FeatureCollectionLayer layer = new FeatureCollectionLayer(featureCollection);
            map.getOperationalLayers().add(layer);

            int[] colors = new int[] {
                0xFFFF0000,
                0xFF00FF00,
                0xFF0000FF,
                0xFFFF00FF,
                0xFFFFFF00,
                0xFF00FFFF,
                0xFFFFA500,
                0xFF8A2BE2,
                0xFF00FFFF,
                0xFFFF1493
            };
            for (int i = 0; i < featureTables.size(); i++) {
              List<Point> points = new ArrayList<>();
              //              points.add(new Point(-1.3041955459030736E7, 3857073.8022902417, ESPG_3857));
              featureTables.get(i).forEach(feature -> {
                points.add((Point) feature.getGeometry());
              });
              try {
                addRoute(points, colors[i]);
              } catch (Exception ex) {
                ex.printStackTrace();
              }
            }

          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });

      } catch (Exception e) {
        e.printStackTrace();
      }

      stackPane.getChildren().addAll(mapView, hboxControl);
      StackPane.setAlignment(hboxControl, Pos.TOP_LEFT);
      StackPane.setMargin(hboxControl, new Insets(10, 0, 0, 10));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private JsonObject getTestAssetJson(String jsonFilePath) throws IOException {

    ByteArrayOutputStream byteArrayOutputStream = getByteArrayOutputStream(jsonFilePath);
    if (byteArrayOutputStream == null)
      return null;

    JsonObject servicesJson = null;
    try {
      JsonParser parser = new JsonParser();
      servicesJson = (JsonObject) parser.parse(byteArrayOutputStream.toString());
    } catch (JsonParseException e) {
      e.printStackTrace();
      return null;
    }
    return servicesJson;
  }

  private ByteArrayOutputStream getByteArrayOutputStream(String filePath) throws IOException {
    InputStream is = null;
    ByteArrayOutputStream byteArrayOutputStream;
    try {
      // Any exceptions reading file should be propogated up the stack, as they will prevent test running correctly.
      is = new FileInputStream(filePath);
      byteArrayOutputStream = new ByteArrayOutputStream();
      int ctr;
      ctr = is.read();
      while (ctr != -1) {
        byteArrayOutputStream.write(ctr);
        ctr = is.read();
      }
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return byteArrayOutputStream;
  }

  private void addRoute(List<Point> points, int color) throws Exception {
    List<Stop> routeStops = routeParameters.getStops();
    SimpleMarkerSymbol stopMarker = new SimpleMarkerSymbol(Style.CIRCLE, color, 14);
    TextSymbol stop1Text = new TextSymbol(10, "1", BLACK, HorizontalAlignment.CENTER,
        VerticalAlignment.MIDDLE);

    routeStops.clear();
    points.forEach(point -> {
      routeStops.add(new Stop(point));
      routeGraphicsOverlay.getGraphics().add(new Graphic(point, stopMarker));
      routeGraphicsOverlay.getGraphics().add(new Graphic(point, stop1Text));
    });

    if (routeStops.size() > 0) {
      RouteResult result = routeTask.solveRouteAsync(routeParameters).get();
      List<Route> routes = result.getRoutes();

      Route route = routes.get(0);
      Geometry shape = route.getRouteGeometry();
      routeGraphic = new Graphic(shape, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, color, 2));
      routeGraphicsOverlay.getGraphics().add(routeGraphic);
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() throws Exception {

    if (mapView != null) {
      mapView.dispose();
    }
  }

  /**
   * Opens and runs application.
   *
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }
}
