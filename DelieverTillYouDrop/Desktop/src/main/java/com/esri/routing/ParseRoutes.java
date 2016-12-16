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

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class ParseRoutes {

  private JsonObject routeInformation;

  public ParseRoutes(String url) throws Exception {
    routeInformation = getTestAssetJson(url);
  }

  public String getRouteInformation() {
    return routeInformation.toString();
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
}
