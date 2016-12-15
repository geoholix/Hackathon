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

import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
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
import com.esri.arcgisruntime.tasks.networkanalysis.DirectionMessageType;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

public class DelieverTillYouDrop extends Application {

  private MapView mapView;

  private RouteTask routeTask;

  private RouteParameters routeParameters;

  private ListView<String> directionsList = new ListView<>();

  private ComboBox<String> comboBox;

  private Graphic routeGraphic;

  private GraphicsOverlay routeGraphicsOverlay = new GraphicsOverlay();

  private final SpatialReference ESPG_3857 = SpatialReference.create(102100);

  private static final int WHITE_COLOR = 0xffffffff;

  private static final int BLUE_COLOR = 0xff0000ff;

  private static final String ROUTE_TASK_SANDIEGO =
      "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route";

  @Override
  public void start(Stage stage) throws Exception {
    try {
      // create stack pane and application scene
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
      // set title, size, and add scene to stage
      stage.setTitle("Deliver Until You Drop");
      stage.setWidth(800);
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
      comboBox = new ComboBox<>(routesList);
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
      // find route
      findButton.setOnAction(e -> {
        try {
          RouteResult result = routeTask.solveRouteAsync(routeParameters).get();
          List<Route> routes = result.getRoutes();
          if (routes.size() < 1) {
            directionsList.getItems().add("No Routes");
          }
          Route route = routes.get(0);
          Geometry shape = route.getRouteGeometry();
          routeGraphic = new Graphic(shape, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, BLUE_COLOR, 2));
          routeGraphicsOverlay.getGraphics().add(routeGraphic);
          // get route street names
          route.getDirectionManeuvers().stream().flatMap(mvr -> mvr.getManeuverMessages().stream())
              .filter(ms -> ms.getType().equals(DirectionMessageType.STREET_NAME))
              .forEach(st -> directionsList.getItems().add(st.getText()));
          resetButton.setDisable(false);
          findButton.setDisable(true);
        } catch (Exception ex) {
          ex.printStackTrace();
        }
      });
      // TODO: clear the routes from the map
      // resetButton
      // add buttons and direction list and label to the control panel
      hboxControl.getChildren().addAll(selectRouteControl, routeControl, barrierControl, findRoutesControl);
      // create a ArcGISMap with a streets basemap
      ArcGISMap map = new ArcGISMap(Basemap.createStreets());
      // set the ArcGISMap to be displayed in this view
      mapView = new MapView();
      mapView.setMap(map);
      // enable find a route button when mapview is done loading
      mapView.addDrawStatusChangedListener(e -> {
        if (e.getDrawStatus() == DrawStatus.COMPLETED) {
          findButton.setDisable(false);
        }
      });
      // set the viewpoint to San Diego (U.S.)
      mapView.setViewpointGeometryAsync(new Envelope(-13067866, 3843014, -13004499, 3871296, ESPG_3857));
      // add the graphic overlay to the map view
      mapView.getGraphicsOverlays().add(routeGraphicsOverlay);
      try {
        // create route task from San Diego service
        routeTask = new RouteTask(ROUTE_TASK_SANDIEGO);
        // load route task
        routeTask.loadAsync();
        routeTask.addDoneLoadingListener(() -> {
          try {
            // get default route parameters
            routeParameters = routeTask.createDefaultParametersAsync().get();
            routeParameters.setOutputSpatialReference(ESPG_3857);
            // set flags to return stops and directions
            routeParameters.setReturnStops(true);
            routeParameters.setReturnDirections(true);
            // set stop locations
            Point stop1Loc = new Point(-1.3018598562659847E7, 3863191.8817135547, ESPG_3857);
            Point stop2Loc = new Point(-1.3036911787723785E7, 3839935.706521739, ESPG_3857);
            // add route stops
            List<Stop> routeStops = routeParameters.getStops();
            routeStops.add(new Stop(stop1Loc));
            routeStops.add(new Stop(stop2Loc));
            // add route stops to the stops overlay
            SimpleMarkerSymbol stopMarker = new SimpleMarkerSymbol(Style.CIRCLE, BLUE_COLOR, 14);
            routeGraphicsOverlay.getGraphics().add(new Graphic(stop1Loc, stopMarker));
            routeGraphicsOverlay.getGraphics().add(new Graphic(stop2Loc, stopMarker));
            // add order text symbols to the stops
            TextSymbol stop1Text =
                new TextSymbol(10, "1", WHITE_COLOR, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            TextSymbol stop2Text =
                new TextSymbol(10, "2", WHITE_COLOR, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
            routeGraphicsOverlay.getGraphics().add(new Graphic(stop1Loc, stop1Text));
            routeGraphicsOverlay.getGraphics().add(new Graphic(stop2Loc, stop2Text));
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
      // add the map view and control panel to stack pane
      stackPane.getChildren().addAll(mapView, hboxControl);
      StackPane.setAlignment(hboxControl, Pos.TOP_LEFT);
      StackPane.setMargin(hboxControl, new Insets(10, 0, 0, 10));
    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
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
