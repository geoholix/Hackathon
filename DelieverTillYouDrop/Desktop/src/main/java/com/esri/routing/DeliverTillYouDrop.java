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

import java.util.ArrayList;
import java.util.List;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

public class DeliverTillYouDrop extends Application {

  private MapView mapView;

  private RouteTask routeTask;

  private RouteParameters routeParameters;

  private ListView<String> directionsList = new ListView<>();

  private Graphic routeGraphic;

  private List<Point> route1;

  private List<Point> route2;

  private List<Point> route3;

  private List<Point> route4;

  private GraphicsOverlay routeGraphicsOverlay = new GraphicsOverlay();

  private final SpatialReference ESPG_3857 = SpatialReference.create(102100);

  private static final int WHITE_COLOR = 0xffffffff;

  private static final int BLUE_COLOR = 0xFF0000FF;

  private static final int GREEN_COLOR = 0xFF00FF00;

  private static final int YELLOW_COLOR = 0xFFFFFF00;

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
      HBox hboxControl = new HBox(10);
      hboxControl.setMaxSize(580, 30);
      hboxControl.getStyleClass().add("panel-region");
      VBox selectRouteControl = new VBox(6);
      selectRouteControl.setMaxSize(250, 30);
      selectRouteControl.getStyleClass().add("panel-region");
      Label routesLabel = new Label("Select the Routes to show:");
      routesLabel.getStyleClass().add("panel-label");
      // create list of routes
      ObservableList<String> routesList = FXCollections.observableArrayList();
      routesList.add("All Routes");
      routesList.add("Route 1");
      routesList.add("Route 2");
      routesList.add("Route 3");
      // create combo box
      ComboBox<String> comboBox = new ComboBox<>(routesList);
      comboBox.setMaxWidth(Double.MAX_VALUE);
      comboBox.setDisable(false);
      // Handle ComboBox event.
      comboBox.setOnAction((event) -> {
        // clears the graphics
        routeGraphicsOverlay.getGraphics().clear();
        // Get selected route(s)
        String selectedRoute = comboBox.getSelectionModel().getSelectedItem();
        // System.out.println("ComboBox Action (selected: " + selectedRoute + ")");
        try {
          if (selectedRoute.equals("Route 1"))
            addRoute(route1, GREEN_COLOR);
          if (selectedRoute.equals("Route 2"))
            addRoute(route2, YELLOW_COLOR);
          if (selectedRoute.equals("Route 3"))
            addRoute(route3, BLUE_COLOR);
          if (selectedRoute.equals("All Routes")) {
            addRoute(route1, GREEN_COLOR);
            addRoute(route2, YELLOW_COLOR);
            addRoute(route3, BLUE_COLOR);
          }
        } catch (Exception e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      });
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
      routeControl.setMaxSize(200, 30);
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
      barrierControl.setMaxSize(200, 30);
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
      findRoutesControl.setMaxSize(200, 30);
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
            mapView.setOnMouseClicked(e -> {
              if (e.getButton() == MouseButton.PRIMARY) {
                Point2D point = new Point2D(e.getX(), e.getY());
                Point mapPoint = mapView.screenToLocation(point);
                System.out.println("X: " + mapPoint.getX() + " Y: " + mapPoint.getY());
              }
            });
            routeParameters = routeTask.createDefaultParametersAsync().get();
            routeParameters.setOutputSpatialReference(ESPG_3857);
            routeParameters.setReturnStops(true);
            routeParameters.setReturnDirections(true);
            createRoutes();
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

  private void createRoutes() {
    route1 = new ArrayList<>();
    route1.add(new Point(-1.304321859640959E7, 3857650.556262654, ESPG_3857));
    route1.add(new Point(-1.3043212577507617E7, 3857742.8460929478, ESPG_3857));
    route1.add(new Point(-1.3043113377983175E7, 3857815.451480437, ESPG_3857));
    route1.add(new Point(-1.3043013444223E7, 3857867.451071079, ESPG_3857));
    route2 = new ArrayList<>();
    route2.add(new Point(-1.3043203549154652E7, 3857485.036458323, ESPG_3857));
    route2.add(new Point(-1.3043211574357286E7, 3857408.7970332974, ESPG_3857));
    route2.add(new Point(-1.30431915113507E7, 3857214.1858694176, ESPG_3857));
    route2.add(new Point(-1.3043019972644394E7, 3857176.0661569047, ESPG_3857));
    route3 = new ArrayList<>();
    route3.add(new Point(-1.3042825361480514E7, 3857668.6129685817, ESPG_3857));
    route3.add(new Point(-1.3042791254369318E7, 3857768.9280015095, ESPG_3857));
    route3.add(new Point(-1.3042709999192646E7, 3857857.205230486, ESPG_3857));
    route3.add(new Point(-1.3042793260669976E7, 3857911.3753482676, ESPG_3857));
    route3.add(new Point(-1.3042824358330185E7, 3858018.7124335007, ESPG_3857));
    route4 = new ArrayList<>();
    route4.add(new Point(-1.3043095208919091E7, 3858187.2416888196, ESPG_3857));
    route4.add(new Point(-1.3043014956892747E7, 3858295.581924382, ESPG_3857));
    route4.add(new Point(-1.304291564501015E7, 3858290.5661727358, ESPG_3857));
    route4.add(new Point(-1.3042880534748625E7, 3858180.219636515, ESPG_3857));
    route4.add(new Point(-1.3042756144107793E7, 3858187.2416888196, ESPG_3857));
  }

  private void addRoute(List<Point> points, int color) throws Exception {
    List<Stop> routeStops = routeParameters.getStops();
    routeStops.clear();
    routeStops.add(new Stop(points.get(0)));
    routeStops.add(new Stop(points.get(1)));
    routeStops.add(new Stop(points.get(2)));
    routeStops.add(new Stop(points.get(3)));
    SimpleMarkerSymbol stopMarker = new SimpleMarkerSymbol(Style.CIRCLE, color, 14);
    routeGraphicsOverlay.getGraphics().add(new Graphic(points.get(0), stopMarker));
    routeGraphicsOverlay.getGraphics().add(new Graphic(points.get(1), stopMarker));
    routeGraphicsOverlay.getGraphics().add(new Graphic(points.get(2), stopMarker));
    routeGraphicsOverlay.getGraphics().add(new Graphic(points.get(3), stopMarker));
    TextSymbol stop1Text = new TextSymbol(10, "1", WHITE_COLOR, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
    TextSymbol stop2Text = new TextSymbol(10, "2", WHITE_COLOR, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
    TextSymbol stop3Text = new TextSymbol(10, "3", WHITE_COLOR, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
    TextSymbol stop4Text = new TextSymbol(10, "4", WHITE_COLOR, HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
    routeGraphicsOverlay.getGraphics().add(new Graphic(points.get(0), stop1Text));
    routeGraphicsOverlay.getGraphics().add(new Graphic(points.get(1), stop2Text));
    routeGraphicsOverlay.getGraphics().add(new Graphic(points.get(2), stop3Text));
    routeGraphicsOverlay.getGraphics().add(new Graphic(points.get(3), stop4Text));
    RouteResult result = routeTask.solveRouteAsync(routeParameters).get();
    List<Route> routes = result.getRoutes();
    Route route = routes.get(0);
    Geometry shape = route.getRouteGeometry();
    routeGraphic = new Graphic(shape, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, color, 2));
    routeGraphicsOverlay.getGraphics().add(routeGraphic);
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
