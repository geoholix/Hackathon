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
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
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

public class DeliverTillYouDrop extends Application {

  private MapView mapView;
  private RouteTask routeTask;
  private RouteParameters routeParameters;
  private ListView<String> directionsList = new ListView<>();

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
      StackPane stackPane = new StackPane();
      Scene scene = new Scene(stackPane);
      scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

      stage.setTitle("DelieverTillYouDrop");
      stage.setWidth(800);
      stage.setHeight(700);
      stage.setScene(scene);
      stage.show();

      VBox vBoxControl = new VBox(6);
      vBoxControl.setMaxSize(200, 300);
      vBoxControl.getStyleClass().add("panel-region");

      Label directionsLabel = new Label("Route directions:");
      directionsLabel.getStyleClass().add("panel-label");

      Button findButton = new Button("Find route");
      findButton.setMaxWidth(Double.MAX_VALUE);
      findButton.setDisable(true);
      Button resetButton = new Button("Reset");
      resetButton.setMaxWidth(Double.MAX_VALUE);
      resetButton.setDisable(true);

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

          route.getDirectionManeuvers().stream().flatMap(mvr -> mvr.getManeuverMessages().stream()).filter(ms -> ms
              .getType().equals(DirectionMessageType.STREET_NAME)).forEach(st -> directionsList.getItems().add(st
                  .getText()));

          resetButton.setDisable(false);
          findButton.setDisable(true);

        } catch (Exception ex) {
          ex.printStackTrace();
        }
      });

      resetButton.setOnAction(e -> {
        routeGraphicsOverlay.getGraphics().remove(routeGraphic);
        directionsList.getItems().clear();
        resetButton.setDisable(true);
        findButton.setDisable(false);
      });

      vBoxControl.getChildren().addAll(directionsLabel, directionsList, findButton, resetButton);

      ArcGISMap map = new ArcGISMap(Basemap.createStreets());

      mapView = new MapView();
      mapView.setMap(map);

      mapView.addDrawStatusChangedListener(e -> {
        if (e.getDrawStatus() == DrawStatus.COMPLETED) {
          findButton.setDisable(false);
        }
      });

      mapView.setViewpointGeometryAsync(new Envelope(-13067866, 3843014, -13004499, 3871296, ESPG_3857));

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

            Point stop1Loc = new Point(-1.3018598562659847E7, 3863191.8817135547, ESPG_3857);
            Point stop2Loc = new Point(-1.3036911787723785E7, 3839935.706521739, ESPG_3857);

            List<Stop> routeStops = routeParameters.getStops();
            routeStops.add(new Stop(stop1Loc));
            routeStops.add(new Stop(stop2Loc));

            SimpleMarkerSymbol stopMarker = new SimpleMarkerSymbol(Style.CIRCLE, BLUE_COLOR, 14);
            routeGraphicsOverlay.getGraphics().add(new Graphic(stop1Loc, stopMarker));
            routeGraphicsOverlay.getGraphics().add(new Graphic(stop2Loc, stopMarker));

            TextSymbol stop1Text = new TextSymbol(10, "1", WHITE_COLOR, HorizontalAlignment.CENTER,
                VerticalAlignment.MIDDLE);
            TextSymbol stop2Text = new TextSymbol(10, "2", WHITE_COLOR, HorizontalAlignment.CENTER,
                VerticalAlignment.MIDDLE);
            routeGraphicsOverlay.getGraphics().add(new Graphic(stop1Loc, stop1Text));
            routeGraphicsOverlay.getGraphics().add(new Graphic(stop2Loc, stop2Text));

          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });

      } catch (Exception e) {
        e.printStackTrace();
      }

      stackPane.getChildren().addAll(mapView, vBoxControl);
      StackPane.setAlignment(vBoxControl, Pos.TOP_LEFT);
      StackPane.setMargin(vBoxControl, new Insets(10, 0, 0, 10));

    } catch (Exception e) {
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
