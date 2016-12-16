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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseButton;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.FeatureCollection;
import com.esri.arcgisruntime.data.FeatureCollectionTable;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol.Style;
import com.esri.arcgisruntime.symbology.TextSymbol;
import com.esri.arcgisruntime.symbology.TextSymbol.HorizontalAlignment;
import com.esri.arcgisruntime.symbology.TextSymbol.VerticalAlignment;
import com.esri.arcgisruntime.tasks.networkanalysis.PointBarrier;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

public class DeliverTillYouDropController {

  @FXML private MapView mapView;
  @FXML private ComboBox<String> comboBox;
  @FXML private Button addRouteButton;
  @FXML private Button removeRouteButton;
  @FXML private Button addBarrierButton;
  @FXML private Button removeBarrierButton;

  private boolean isAddingBarriers = false;
  private boolean isRemovingBarriers = false;

  private int[] colors = new int[] {
      0xFFFF0000, 0xFF00FF00, 0xFF0000FF,
      0xFFFF00FF, 0xFFFFFF00, 0xFF00FFFF,
      0xFFFFA500, 0xFF8A2BE2, 0xFF93FF14,
      0xFFFF1493
  };
  private List<Route> routes = new ArrayList<>();
  private ParseRoutes routeParser;
  private Executor threadPool = Executors.newSingleThreadExecutor();

  private SimpleMarkerSymbol barrierMarker = new SimpleMarkerSymbol(Style.CIRCLE, 0xFF000000, 14);

  private List<FeatureCollectionTable> featureTables;
  private List<Point> barrierPoints = new ArrayList<>();
  private List<List<Point>> routePoints = new ArrayList<>();
  private GraphicsOverlay routeGraphicsOverlay;
  private GraphicsOverlay barrierGraphicsOverlay;
  private RouteParameters routeParameters;
  private RouteTask routeTask;
  private final SpatialReference ESPG_3857 = SpatialReference.create(102100);

  public void initialize() {

    ObservableList<String> routesList = FXCollections.observableArrayList();
    routesList.add("All Routes");
    routesList.add("Red Route");
    routesList.add("Green Route");
    routesList.add("Blue Route");
    routesList.add("Magenta Route");
    routesList.add("Yellow Route");
    routesList.add("Aqua Route");
    routesList.add("Orange Route");
    routesList.add("Purple Route");
    routesList.add("Lime Green Route");
    routesList.add("Pink Route");
    comboBox.getItems().addAll(routesList);

    try {
      routeParser = new ParseRoutes(getClass().getResource("/deliveries.txt").getPath());

      ArcGISMap map = new ArcGISMap(Basemap.createStreets());
      mapView.setMap(map);

      mapView.setViewpointCenterAsync(new Point(-1.3042962793075608E7, 3857768.9280015198, ESPG_3857), 10000);
      routeGraphicsOverlay = new GraphicsOverlay();
      barrierGraphicsOverlay = new GraphicsOverlay();
      mapView.getGraphicsOverlays().add(routeGraphicsOverlay);
      mapView.getGraphicsOverlays().add(barrierGraphicsOverlay);

      String ROUTE_TASK_SANDIEGO =
          "http://sampleserver6.arcgisonline.com/arcgis/rest/services/NetworkAnalysis/SanDiego/NAServer/Route";
      routeTask = new RouteTask(ROUTE_TASK_SANDIEGO);
      routeTask.addDoneLoadingListener(() -> {
        ListenableFuture<RouteParameters> listener = routeTask.createDefaultParametersAsync();
        listener.addDoneListener(() -> {
          try {
            routeParameters = listener.get();
            routeParameters.setOutputSpatialReference(ESPG_3857);
            threadPool.execute(this::createRoutes);
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });
      });
      routeTask.loadAsync();

      FeatureCollection featureCollection = FeatureCollection.fromJson(routeParser.getRouteInformation());
      featureTables = featureCollection.getTables();

      addRemoveBarrierControls();
      setupMapViewInteraction();
      addRouteSelectionControls();

      // adding route
      //      addRouteButton.setOnAction(e -> {
      //        final ArrayList<Field> fields = new ArrayList<Field>();
      //        fields.add(Field.createString("Name", "Name", 50));
      //        FeatureCollectionTable mRoute =
      //            new FeatureCollectionTable(fields, GeometryType.POINT, SpatialReferences.getWebMercator());
      //        mRoutes.add(mRoute);
      //      });

      //      removeRouteButton.setOnAction(e -> {
      //      });

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void createRoutes() {

    for (int i = 0; i < featureTables.size(); i++) {
      List<Point> points = new ArrayList<>();
      //              points.add(new Point(-1.3041955459030736E7, 3857073.8022902417, ESPG_3857));
      featureTables.get(i).forEach(feature -> {
        points.add((Point) feature.getGeometry());
      });
      routePoints.add(points);
      addRoute(points, colors[i]);
    }
  }

  private void updateRoute() {

    routeGraphicsOverlay.getGraphics().clear();
    barrierGraphicsOverlay.getGraphics().clear();

    barrierPoints.forEach(point -> {
      barrierGraphicsOverlay.getGraphics().add(new Graphic(point, barrierMarker));
    });
    for (int i = 0; i < routePoints.size(); i++) {
      addRoute(routePoints.get(i), colors[i]);
    }
  }

  private void addRoute(List<Point> points, int color) {

    List<Stop> routeStops = routeParameters.getStops();
    //    List<Stop> routeStops = new ArrayList<>();
    SimpleMarkerSymbol stopMarker = new SimpleMarkerSymbol(Style.CIRCLE, color, 14);
    TextSymbol stop1Text = new TextSymbol(10, "1", 0xFF000000, HorizontalAlignment.CENTER,
        VerticalAlignment.MIDDLE);

    routeStops.clear();
    points.forEach(point -> {
      routeStops.add(new Stop(point));
      routeGraphicsOverlay.getGraphics().add(new Graphic(point, stopMarker));
      routeGraphicsOverlay.getGraphics().add(new Graphic(point, stop1Text));
    });

    routeParameters.getPointBarriers().clear();
    barrierPoints.forEach(point -> {
      PointBarrier barrier = new PointBarrier(point);
      routeParameters.getPointBarriers().add(barrier);
    });

    if (routeStops.size() > 0) {
      try {
        RouteResult result = routeTask.solveRouteAsync(routeParameters).get();
        Route route = result.getRoutes().get(0);
        routes.add(route);

        // create route trip on map
        Geometry shape = route.getRouteGeometry();
        Graphic routeGraphic = new Graphic(shape, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, color, 2));
        routeGraphicsOverlay.getGraphics().add(routeGraphic);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void addRemoveBarrierControls() {

    addBarrierButton.setOnAction(e -> {
      if (!isAddingBarriers) {
        isAddingBarriers = true;
      } else {
        isAddingBarriers = false;
        threadPool.execute(this::updateRoute);
      }
    });

    removeBarrierButton.setOnAction(e -> {
      if (!isRemovingBarriers) {
        isRemovingBarriers = true;
      } else {
        isRemovingBarriers = false;
        threadPool.execute(this::updateRoute);
      }
    });
  }

  private void setupMapViewInteraction() {

    mapView.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.PRIMARY) {

        Point2D point = new Point2D(e.getX(), e.getY());
        Point mapPoint = mapView.screenToLocation(point);

        if (isAddingBarriers) {
          barrierPoints.add(mapPoint);
          barrierGraphicsOverlay.getGraphics().add(new Graphic(mapPoint, barrierMarker));
        } else if (isRemovingBarriers) {
          Point2D screenPoint = mapView.locationToScreen(mapPoint);
          ListenableFuture<IdentifyGraphicsOverlayResult> identifyTask =
              mapView.identifyGraphicsOverlayAsync(barrierGraphicsOverlay, screenPoint, 30, false, 1);
          identifyTask.addDoneListener(() -> {
            try {
              List<Graphic> graphics = identifyTask.get().getGraphics();
              if (graphics.size() > 0) {
                Graphic graphic = graphics.get(0);
                graphic.setSelected(true);
                barrierGraphicsOverlay.getGraphics().remove(graphic);
                barrierPoints.remove(graphic.getGeometry());
              }
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          });
        }
      }
    });
  }

  private void addRouteSelectionControls() {

    comboBox.setOnAction((event) -> {
      // clears the graphics
      // Get selected route(s)
      int selectedRoute = (comboBox.getSelectionModel().getSelectedIndex() - 1);
      //        addRoute(routes.get(0), GREEN_COLOR);
      if (selectedRoute != -1) {
        routeGraphicsOverlay.getGraphics().clear();
        Geometry shape = routes.get(selectedRoute).getRouteGeometry();
        Graphic routeGraphic =
            new Graphic(shape, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, colors[selectedRoute], 2));
        routeGraphicsOverlay.getGraphics().add(routeGraphic);

        SimpleMarkerSymbol stopMarker = new SimpleMarkerSymbol(Style.CIRCLE, colors[selectedRoute], 14);
        TextSymbol stop1Text = new TextSymbol(10, "1", 0xFF000000, HorizontalAlignment.CENTER,
            VerticalAlignment.MIDDLE);
        routePoints.get(selectedRoute).forEach(point -> {
          routeGraphicsOverlay.getGraphics().add(new Graphic(point, stopMarker));
          routeGraphicsOverlay.getGraphics().add(new Graphic(point, stop1Text));
        });
      } else {
        threadPool.execute(this::updateRoute);
      }
    });
  }

  /**
   * Disposes application resources.
   */
  void terminate() {
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
