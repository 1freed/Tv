package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.crawler.Spider;
import com.github.catvod.parser.Base64Utils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.List;


public class Upyunso extends Spider {
    private static String b = "https://www.upyunso.com/";

    @Override
    public void init(Context context, String extend) {
        super.init(context, extend);
    }

    @Override
    public String detailContent(List<String> list) {
        try {
            String id = list.get(0);
            String[] idInfo = id.split("\\$\\$\\$");
            String url = idInfo[0];
            String pic = idInfo[1];
            String title = idInfo[2];
            url = Base64Utils.sendGet(url);
            list.set(0, url + "$$$" + pic + "$$$" + title);
            return PushAgent.getDetail(list);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String playerContent(String str, String str2, List<String> list) {
        return PushAgent.player(str, str2, list);
    }

    @Override
    public String searchContent(String key, boolean z) {
        try {
            JSONArray jSONArray = new JSONArray();
            JSONArray arr = Base64Utils.getJSONByUrl("https://api.upyunso.com/search?keyword=" + key);
            String pic = "http://image.xinjun58.com/image/tv/ups.jpg";
            for (int i = 0; i < arr.length(); i++) {
                JSONObject v = arr.getJSONObject(i);
                String id = v.getString("url");
                String title = v.getString("title");
                v.put("vod_id", id + "$$$" + pic + "$$$" + title);
                v.put("vod_name", title);
                v.put("remark", "");
                v.put("vod_pic", pic);
                jSONArray.put(v);
            }
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("list", jSONArray);
            return jSONObject2.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}