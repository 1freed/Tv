package com.github.catvod.spider;

import android.content.Context;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Misc;
import com.github.catvod.utils.okhttp.OkHttpUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

public class PushAgentQQ extends Spider {
    protected JSONObject rule = null;
    @Override
    public void init(Context context, String extend) {
        super.init(context, extend);
        if (extend != null && !extend.equals("")) {
            if (extend.startsWith("http")) {
                Misc.jsonUrl = extend;
            }
        }
        PushAgent.fetchRule(false, 0);
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            fetchRule(true);
            JSONObject result = new JSONObject();
            JSONArray classes = new JSONArray();
            String[] fenleis = PushAgent.getRuleVal(rule,"fenlei", "").split("#");
            for (String fenlei : fenleis) {
                String[] info = fenlei.split("\\$");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type_name", info[0]);
                jsonObject.put("type_id", info[1]);
                classes.put(jsonObject);
            }
            result.put("class", classes);
            return result.toString();
        } catch (
                Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }
    protected JSONObject fetchRule(boolean flag) {
        rule = PushAgent.fetchRule(flag, 0);
        return rule;
    }
    @Override
    public String homeVideoContent() {
        try {
            JSONObject jo = PushAgent.fetchRule(true,1);
            JSONArray videos = new JSONArray();
            String[] fenleis = PushAgent.getRuleVal(jo, "fenlei", "").split("#");
            for (String fenlei : fenleis) {
                String[] info = fenlei.split("\\$");
                JSONObject data = category(info[1], "1", false, new HashMap<>(),jo);
                if (data != null) {
                    JSONArray vids = data.optJSONArray("list");
                    if (vids != null) {
                        for (int i = 0; i < vids.length() && i < 5; i++) {
                            videos.put(vids.getJSONObject(i));
                        }
                    }
                }
                if (videos.length() >= 30)
                    break;
            }
            JSONObject result = new JSONObject();
            result.put("list", videos);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private JSONObject category(String tid, String pg, boolean filter, HashMap<String, String> extend,JSONObject jo) {
        try {
            JSONArray videos = new JSONArray();
            String url=null,name=null,pic=null;
            JSONObject jsonObject = null, v = null;
            if (tid.equals("bili")) {
                String json = OkHttpUtil.string("https://api.bilibili.com/x/web-interface/ranking/v2?rid=0&type=all", null);
                JSONObject j = new JSONObject(json);
                JSONObject o = j.getJSONObject("data");
                JSONArray array = o.getJSONArray("list");
                for (int i = 0; i < array.length(); i++) {
                    jsonObject = array.getJSONObject(i);
                    url = jsonObject.optString("short_link", "");
                    name = jsonObject.optString("title", "");
                    pic = jsonObject.optString("pic", "");
                    v = new JSONObject();
                    v.put("vod_id", url + "$$$" + pic + "$$$" + name);
                    v.put("vod_name", name);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", "");
                    videos.put(v);
                }
            }else{
                if (jo == null) jo = PushAgent.fetchRule(true,1);
                JSONArray array = jo.getJSONArray(tid);
                for (int i = 0; i < array.length(); i++) {
                    jsonObject = array.getJSONObject(i);
                    url = PushAgent.getRuleVal(jsonObject, "url");
                    name = PushAgent.getRuleVal(jsonObject, "name");
                    pic = PushAgent.getRuleVal(jsonObject, "pic");
                    if(pic.equals("")) pic = Misc.getWebName(url, 1);
                    v = new JSONObject();
                    v.put("vod_id", url + "$$$" + pic + "$$$" + name);
                    v.put("vod_name", name);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", Misc.getWebName(url,0));
                    videos.put(v);
                }
            }
            JSONObject result = new JSONObject();
            result.put("page", pg);
            result.put("pagecount", Integer.MAX_VALUE);
            result.put("limit", 120);
            result.put("total", Integer.MAX_VALUE);
            result.put("list", videos);
            return result;
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return null;
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        JSONObject obj = category(tid, pg, filter, extend,null);
        return obj != null ? obj.toString() : "";
    }

    @Override
    public String detailContent(List<String> list) {
        return PushAgent.getDetail(list);
    }


    @Override
    public String playerContent(String str, String str2, List<String> list) {
        return PushAgent.player(str, str2, list);
    }

    @Override
    public  String searchContent(String key, boolean quick) {
        JSONObject result = new JSONObject();
        JSONArray videos = new JSONArray();
        try {
            fetchRule(true);
            String url = "",webUrl,detailRex,siteUrl,siteName;
            JSONArray siteArray = rule.getJSONArray("sites");
            for (int j = 0; j < siteArray.length(); j++) {
                JSONObject site = siteArray.getJSONObject(j);
                siteUrl = site.optString("site");
                siteName = site.optString("name");
                detailRex = siteUrl+site.optString("detailRex","/vod/%s.html");
                webUrl = siteUrl + "/index.php/ajax/suggest?mid=1&wd="+key;

                JSONObject data = new JSONObject(OkHttpUtil.string(webUrl, Misc.Headers(0)));
                JSONArray vodArray = data.getJSONArray("list");
                for (int i = 0; i < vodArray.length(); i++) {
                    JSONObject vod = vodArray.getJSONObject(i);
                    String name = vod.optString("name").trim();
                    String id = vod.optString("id").trim();
                    String pic = vod.optString("pic").trim();
                    pic = Misc.fixUrl(webUrl, pic);

                    url = detailRex.replace("%s",id);
                    JSONObject v = new JSONObject();
                    v.put("vod_id", url + "$$$" + pic + "$$$" + name);
                    v.put("vod_name", "["+siteName+"]"+name);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", "");
                    videos.put(v);
                }
            }
            result.put("list", videos);
            return result.toString();
        } catch (Exception e) {
            SpiderDebug.log(e);
            if (videos.length() > 0) {
                try {
                    result.put("list", videos);
                } catch (JSONException jsonException) {
                    jsonException.printStackTrace();
                }
                return result.toString();
            }
        }
        return "";
    }
}