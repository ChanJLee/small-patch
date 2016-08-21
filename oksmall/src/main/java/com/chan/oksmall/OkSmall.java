package com.chan.oksmall;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.chan.ypatchcore.YPatch;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import net.wequick.small.Small;
import net.wequick.small.util.FileUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

/**
 * Created by chan on 16/8/19.
 */
public class OkSmall {

    private static final String BUNDLE_MANIFEST_NAME = "bundle.json";
    private static final String KEY_BUNDLE = "bundles";
    private static final String KEY_URI = "uri";
    private static final String KEY_PKG = "pkg";
    private static final String TAG = "OkSmall";

    private static File sPatchManifestFile;

    /**
     * @param context
     * @param pluginUri bundle.json bundles每个数组元素的uri
     * @param uri 远程服务器端增量包的uri
     */
    public static void merge(Context context, String pluginUri, String uri) {

        String packageName = parsePackageName(context, pluginUri);
        if (TextUtils.isEmpty(packageName)) {
            Log.e(TAG, "can't parse plugin's package name, check if assets/bundle.json is deleted");
            return;
        }

        // small框架的约定就是，插件名字是以lib+包名.so明明的
        // 比如如果我们插件的包名是com.chan.app.setting
        // 那么对于插件名就是libcom_chan_app_setting.so
        final String soName = "lib" + packageName.replaceAll("\\.", "_") + ".so";

        //找到旧版本的插件安装包
        final File oldPlugin = new File(context.getApplicationInfo().nativeLibraryDir, soName);
        //新版本都存放到small指定的download目录下
        final File newPlugin = new File(FileUtils.getDownloadBundlePath(), soName);

        //开始下载patch
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(uri).build();

        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {

            }

            @Override
            public void onResponse(Response response) throws IOException {

                //保存从服务器端下载的增量包
                File patch = new File(FileUtils.getDownloadBundlePath(), "patch.so");
                FileOutputStream fileOutputStream = new FileOutputStream(patch);
                byte[] content = response.body().bytes();
                fileOutputStream.write(content);
                fileOutputStream.flush();
                fileOutputStream.close();

                //合成的新插件安装包存放文职有new Plugin指定
                YPatch.patch(oldPlugin.getAbsolutePath(), newPlugin.getAbsolutePath(), patch.getAbsolutePath());
            }
        });
    }

    private static String parsePackageName(Context context, String pluginUri) {
        JSONObject manifest = parseManifest(context);
        if (manifest == null) {
            return null;
        }

        if (manifest.has(KEY_BUNDLE)) {
            try {
                JSONArray array = manifest.getJSONArray(KEY_BUNDLE);
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject object = array.getJSONObject(i);
                    if (pluginUri.equals(object.getString(KEY_URI))) {
                        return object.getString(KEY_PKG);
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private static JSONObject parseManifest(Context context) {

        try {
            File patchManifestFile = getPatchManifestFile();
            String manifestJson = getCacheManifest();
            if (manifestJson != null) {
                // Load from cache and save as patch
                if (!patchManifestFile.exists()) patchManifestFile.createNewFile();
                PrintWriter pw = new PrintWriter(new FileOutputStream(patchManifestFile));
                pw.print(manifestJson);
                pw.flush();
                pw.close();

            } else if (patchManifestFile.exists()) {
                // Load from patch
                BufferedReader br = new BufferedReader(new FileReader(patchManifestFile));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();
                manifestJson = sb.toString();
            } else {
                // Load from built-in `assets/bundle.json'
                InputStream builtinManifestStream = context.getAssets().open(BUNDLE_MANIFEST_NAME);
                int builtinSize = builtinManifestStream.available();
                byte[] buffer = new byte[builtinSize];
                builtinManifestStream.read(buffer);
                builtinManifestStream.close();
                manifestJson = new String(buffer, 0, builtinSize);
            }

            // Parse manifest file
            return new JSONObject(manifestJson);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static File getPatchManifestFile() {
        if (sPatchManifestFile == null) {
            synchronized (OkSmall.class) {
                if (sPatchManifestFile == null) {
                    sPatchManifestFile = new File(Small.getContext().getFilesDir(), BUNDLE_MANIFEST_NAME);
                }
            }
        }
        return sPatchManifestFile;
    }

    private static String getCacheManifest() {
       return Small.getSharedPreferences().getString(BUNDLE_MANIFEST_NAME, null);
    }
}
