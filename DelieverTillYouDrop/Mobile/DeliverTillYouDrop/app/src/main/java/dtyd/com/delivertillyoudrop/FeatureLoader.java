package dtyd.com.delivertillyoudrop;

import android.content.Context;
import android.content.res.AssetManager;

import com.esri.arcgisruntime.data.FeatureCollection;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by cj on 15/12/2016.
 */

public class FeatureLoader {
    /**
     * Return a file as a string
     * @param filePath
     * @param assetManager
     * @return
     */
    private static String readFileAsString(String filePath, AssetManager assetManager) {

        try {
            StringBuilder buf = new StringBuilder();
            InputStream json = assetManager.open(filePath);
            BufferedReader in =
                    new BufferedReader(new InputStreamReader(json, "UTF-8"));
            String str;

            while ((str = in.readLine()) != null) {
                buf.append(str);
            }

            in.close();

            return buf.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get a feature collection from assets
     * @param filePath
     * @param assetManager
     * @return
     */
    public static FeatureCollection loadFeature(String filePath, AssetManager assetManager) {
        final String jsonString = readFileAsString(filePath, assetManager);
        return FeatureCollection.fromJson(jsonString);
    }
}
