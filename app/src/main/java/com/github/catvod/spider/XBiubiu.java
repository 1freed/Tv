package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;
import com.github.catvod.crawler.SpiderDebug;
import com.github.catvod.utils.Misc;
import com.github.catvod.utils.okhttp.OKCallBack;
import com.github.catvod.utils.okhttp.OkHttpUtil;
import okhttp3.Call;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XBiubiu extends PushAgent {
    @Override
    public void init(Context context) {
        super.init(context);
    }

    public void init(Context context, String extend) {
        super.init(context, extend);
        this.ext = extend;
        fetchRule(false,0);
    }

    @Override
    public String homeContent(boolean filter) {
        try {
            fetchRule();
            JSONObject result = new JSONObject();
            JSONArray classes = new JSONArray();
            String[] fenleis = getRuleVal("fenlei", "").split("#");
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

    protected HashMap<String, String> getHeaders(String url) {
        HashMap<String, String> headers = new HashMap<>();
        String ua = getRuleVal("ua", Misc.UaWinChrome).trim();
        headers.put("User-Agent", ua);
        return headers;
    }

    @Override
    public String homeVideoContent() {
        try {
            fetchRule();
            String shouye = getRuleVal("shouye", "1");
            if (shouye.equals("1")) {
                JSONArray videos = new JSONArray();
                String[] fenleis = getRuleVal("fenlei", "").split("#");
                for (String fenlei : fenleis) {
                    String[] info = fenlei.split("\\$");
                    JSONObject data = category(info[1], "1", false, new HashMap<>());
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
            }
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    private JSONObject category(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        try {
            fetchRule();
            if (tid.equals("空"))
                tid = "";
            String qishiye = rule.optString("qishiye", "nil");
            if (qishiye.equals("空"))
                pg = "";
            else if (!qishiye.equals("nil")) {
                pg = String.valueOf(Integer.parseInt(pg) - 1 + Integer.parseInt(qishiye));
            }
            String baseUrl = getRuleVal("url");
            String webUrl = baseUrl + tid + pg + getRuleVal("houzhui");
            String html = fetch(webUrl);
            html = removeUnicode(html);
            String parseContent = html;
            String shifouercijiequ = getRuleVal("shifouercijiequ", "1");
            if (shifouercijiequ.equals("1")) {
                String jiequqian = getRuleVal("jiequqian");
                String jiequhou = getRuleVal("jiequhou");
                parseContent = subContent(html, jiequqian, jiequhou).get(0);
            }
            String jiequshuzuqian = getRuleVal("jiequshuzuqian");
            String jiequshuzuhou = getRuleVal("jiequshuzuhou");
            ArrayList<String> li = null;
            JSONArray videos = new JSONArray();
            ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);
            String biaotiqian = getRuleVal("biaotiqian");
            String biaotihou = getRuleVal("biaotihou");
            String sousuohouzhui = getRuleVal("sousuohouzhui");
            String lianjieqian = getRuleVal("lianjieqian");
            String lianjiehou = getRuleVal("lianjiehou");
            String link = null;
            for (int i = 0; i < jiequContents.size(); i++) {
                try {
                    String jiequContent = jiequContents.get(i);
                    String title = removeHtml(subContent(jiequContent, biaotiqian, biaotihou).get(0));
                    String pic = "";
                    String tupianqian = getRuleVal("tupianqian").toLowerCase();
                    if (tupianqian.startsWith("http://") || tupianqian.startsWith("https://")) {
                        pic = getRuleVal("tupianqian");
                    } else {
                        li = subContent(jiequContent, getRuleVal("tupianqian"), getRuleVal("tupianhou"));
                        if (!li.isEmpty()) {
                            pic = li.get(0);
                            pic = Misc.fixUrl(webUrl, pic);
                        }
                    }
                    if(!lianjiehou.equals("")) {
                        link = subContent(jiequContent, lianjieqian, lianjiehou).get(0);
                        link = getRuleVal("ljqianzhui").isEmpty() ? (link + getRuleVal("ljhouzhui")) : ("x:" + getRuleVal("ljqianzhui")) + link + getRuleVal("ljhouzhui");
                    } else link = jiequContent.replaceAll(".*\"(.*"+sousuohouzhui+".*?)\".*","$1");
                    String remark = !getRuleVal("fubiaotiqian").isEmpty() && !getRuleVal("fubiaotihou").isEmpty() ?
                            removeHtml(subContent(jiequContent, getRuleVal("fubiaotiqian"), getRuleVal("fubiaotihou")).get(0)) : "";
                    JSONObject v = new JSONObject();
                    v.put("vod_id", baseUrl+link + "$$$" + pic + "$$$" + title);
                    v.put("vod_name", title);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", remark);
                    videos.put(v);
                } catch (Throwable th) {
                    th.printStackTrace();
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

    private static String removeUnicode(String str) {
        Pattern pattern = Pattern.compile("(\\\\u(\\w{4}))");
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            String full = matcher.group(1);
            String ucode = matcher.group(2);
            char c = (char) Integer.parseInt(ucode, 16);
            str = str.replace(full, c + "");
        }
        return str;
    }

    String removeHtml(String text) {
        return Jsoup.parse(text).text();
    }
    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        JSONObject obj = category(tid, pg, filter, extend);
        return obj != null ? obj.toString() : "";
    }

    @Override
    public String detailContent(List<String> ids) {
        try {
            fetchRule();
            String jiequshuzuqian = getRuleVal("bfjiequshuzuqian");
            boolean bfjiequshuzuqian = jiequshuzuqian.equals("");
            if(!bfjiequshuzuqian){
                String baseUrl = getRuleVal("url");
                String[] idInfo = ids.get(0).split("\\$\\$\\$");
                String webUrl = idInfo[0];
                String html = fetch(webUrl);
                String parseContent = html;
                boolean bfshifouercijiequ = getRuleVal("bfshifouercijiequ").equals("1");
                if (bfshifouercijiequ) {
                    String jiequqian = getRuleVal("bfjiequqian");
                    String jiequhou = getRuleVal("bfjiequhou");
                    parseContent = subContent(html, jiequqian, jiequhou).get(0);
                }

                ArrayList<String> playList = new ArrayList<>();


                String jiequshuzuhou = getRuleVal("bfjiequshuzuhou");
                boolean bfyshifouercijiequ = getRuleVal("bfyshifouercijiequ").equals("1");
                ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);
                for (int i = 0; i < jiequContents.size(); i++) {
                    try {
                        String jiequContent = jiequContents.get(i);
                        String parseJqContent = bfyshifouercijiequ ? subContent(jiequContent, getRuleVal("bfyjiequqian"), getRuleVal("bfyjiequhou")).get(0) : jiequContent;
                        ArrayList<String> lastParseContents = subContent(parseJqContent, getRuleVal("bfyjiequshuzuqian"), getRuleVal("bfyjiequshuzuhou"));
                        List<String> vodItems = new ArrayList<>();
                        for (int j = 0; j < lastParseContents.size(); j++) {
                            String title = subContent(lastParseContents.get(j), getRuleVal("bfbiaotiqian"), getRuleVal("bfbiaotihou")).get(0);
                            String link = subContent(lastParseContents.get(j), getRuleVal("bflianjieqian"), getRuleVal("bflianjiehou")).get(0);
                            vodItems.add(title + "$" + baseUrl + link);
                        }
                        playList.add(0, TextUtils.join("#", vodItems));
                    } catch (Throwable th) {
                        th.printStackTrace();
                        break;
                    }
                }

                String cover = idInfo[1], title = idInfo[2], area = "";
                String director = "";
                String actor = "";
                String desc = "";
                String remark = "";
                String year = "";
                String category = "";

                if (!getRuleVal("leixinqian").isEmpty() && !getRuleVal("leixinhou").isEmpty()) {
                    try {
                        category = subContent(html, getRuleVal("leixinqian"), getRuleVal("leixinhou")).get(0).replaceAll("\\s+", "").replaceAll("\\&[a-zA-Z]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "");
                    } catch (Exception e) {
                        SpiderDebug.log(e);
                    }
                }
                if (!getRuleVal("niandaiqian").isEmpty() && !getRuleVal("niandaihou").isEmpty()) {
                    try {
                        year = subContent(html, getRuleVal("niandaiqian"), getRuleVal("niandaihou")).get(0).replaceAll("\\s+", "").replaceAll("\\&[a-zA-Z]{1,10};", "").replaceAll("<[^>]*>", "").replaceAll("[(/>)<]", "");
                    } catch (Exception e) {
                        SpiderDebug.log(e);
                    }
                }
                if (!getRuleVal("zhuangtaiqian").isEmpty() && !getRuleVal("zhuangtaihou").isEmpty()) {
                    try {
                        remark = subContent(html, getRuleVal("zhuangtaiqian"), getRuleVal("zhuangtaihou")).get(0);
                    } catch (Exception e) {
                        SpiderDebug.log(e);
                    }
                }
                if (!getRuleVal("zhuyanqian").isEmpty() && !getRuleVal("zhuyanhou").isEmpty()) {
                    try {
                        actor = subContent(html, getRuleVal("zhuyanqian"), getRuleVal("zhuyanhou")).get(0).replaceAll("\\s+", "");
                    } catch (Exception e) {
                        SpiderDebug.log(e);
                    }
                }
                if (!getRuleVal("daoyanqian").isEmpty() && !getRuleVal("daoyanhou").isEmpty()) {
                    try {
                        director = subContent(html, getRuleVal("daoyanqian"), getRuleVal("daoyanhou")).get(0).replaceAll("\\s+", "");
                    } catch (Exception e) {
                        SpiderDebug.log(e);
                    }
                }
                if (!getRuleVal("juqingqian").isEmpty() && !getRuleVal("juqinghou").isEmpty()) {
                    try {
                        desc = subContent(html, getRuleVal("juqingqian"), getRuleVal("juqinghou")).get(0);
                        if(desc!=null){
                            desc = desc.replace("\t", "");
                            desc = desc.replaceAll("\\.*>(.*)", "$1");
                        }
                    } catch (Exception e) {
                        SpiderDebug.log(e);
                    }
                }

                JSONObject vod = new JSONObject();
                vod.put("vod_id", ids.get(0));
                vod.put("vod_name", title);
                vod.put("vod_pic", cover);
                vod.put("type_name", category);
                vod.put("vod_year", year);
                vod.put("vod_area", area);
                vod.put("vod_remarks", remark);
                vod.put("vod_actor", actor);
                vod.put("vod_director", director);
                vod.put("vod_content", desc);

                ArrayList<String> playFrom = new ArrayList<>();

                for (int i = 0; i < playList.size(); i++) {
                    playFrom.add("播放列表" + (i + 1));
                }

                String vod_play_from = TextUtils.join("$$$", playFrom);
                String vod_play_url = TextUtils.join("$$$", playList);
                vod.put("vod_play_from", vod_play_from);
                vod.put("vod_play_url", vod_play_url);

                JSONObject result = new JSONObject();
                JSONArray list = new JSONArray();
                list.put(vod);
                result.put("list", list);
                return result.toString();
            } else return PushAgent.getDetail(ids);
        } catch (Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    @Override
    public String searchContent(String key, boolean quick) {
        try {
            fetchRule();
            boolean ssmoshiJson = getRuleVal("ssmoshi").equals("0");
            String baseUrl = getRuleVal("url");
            String webUrlTmp = baseUrl + getRuleVal("sousuoqian") + key + getRuleVal("sousuohou");
            String webUrl = webUrlTmp.split(";")[0];
            String webContent = webUrlTmp.contains(";post") ? fetchPost(webUrl) : fetch(webUrl);
            JSONObject result = new JSONObject();
            JSONArray videos = new JSONArray();
            if (ssmoshiJson) {
                JSONObject data = new JSONObject(webContent);
                JSONArray vodArray = data.getJSONArray("list");
                for (int j = 0; j < vodArray.length(); j++) {
                    JSONObject vod = vodArray.getJSONObject(j);
                    String name = vod.optString(getRuleVal("jsname")).trim();
                    String id = vod.optString(getRuleVal("jsid")).trim();
                    String pic = vod.optString(getRuleVal("jspic")).trim();
                    pic = Misc.fixUrl(webUrl, pic);
                    JSONObject v = new JSONObject();
                    v.put("vod_id", baseUrl+getRuleVal("sousuohouzhui") + id + "$$$" + pic + "$$$" + name);
                    v.put("vod_name", name);
                    v.put("vod_pic", pic);
                    v.put("vod_remarks", "");
                    videos.put(v);
                }
            } else {
                String parseContent = webContent;
                boolean shifouercijiequ = getRuleVal("sousuoshifouercijiequ").equals("1");
                if (shifouercijiequ) {
                    String jiequqian = getRuleVal("ssjiequqian");
                    String jiequhou = getRuleVal("ssjiequhou");
                    parseContent = subContent(webContent, jiequqian, jiequhou).get(0);
                }
                String jiequshuzuqian = getRuleVal("ssjiequshuzuqian");
                String jiequshuzuhou = getRuleVal("ssjiequshuzuhou");
                ArrayList<String> jiequContents = subContent(parseContent, jiequshuzuqian, jiequshuzuhou);
                for (int i = 0; i < jiequContents.size(); i++) {
                    try {
                        String jiequContent = jiequContents.get(i);
                        String title = subContent(jiequContent, getRuleVal("ssbiaotiqian"), getRuleVal("ssbiaotihou")).get(0);
                        String pic = subContent(jiequContent, getRuleVal("sstupianqian"), getRuleVal("sstupianhou")).get(0);
                        pic = Misc.fixUrl(webUrl, pic);
                        String link = subContent(jiequContent, getRuleVal("sslianjieqian"), getRuleVal("sslianjiehou")).get(0);
                        JSONObject v = new JSONObject();
                        v.put("vod_id", baseUrl+link + "$$$" + pic + "$$$" + title);
                        v.put("vod_name", title);
                        v.put("vod_pic", pic);
                        v.put("vod_remarks", "");
                        videos.put(v);
                    } catch (Throwable th) {
                        th.printStackTrace();
                        break;
                    }
                }
            }
            result.put("list", videos);
            return result.toString();
        } catch (
                Exception e) {
            SpiderDebug.log(e);
        }
        return "";
    }

    protected String ext = null;
    protected JSONObject rule = null;

    protected void fetchRule() {
        if (rule == null) {
            if (ext != null) {
                try {
                    if (ext.startsWith("http")) {
                        String json = OkHttpUtil.string(ext, null);
                        rule = new JSONObject(json);
                    } else {
                        rule = new JSONObject(ext);
                    }
                } catch (JSONException e) {
                }
            }
        }
    }

    protected String fetch(String webUrl) {
        SpiderDebug.log(webUrl);
        return OkHttpUtil.string(webUrl, getHeaders(webUrl)).replaceAll("\r|\n", "");
    }

    protected String fetchPost(String webUrl) {
        SpiderDebug.log(webUrl);
        OKCallBack.OKCallBackString callBack = new OKCallBack.OKCallBackString() {
            @Override
            protected void onFailure(Call call, Exception e) {

            }

            @Override
            protected void onResponse(String response) {
            }
        };
        OkHttpUtil.post(OkHttpUtil.defaultClient(), webUrl, callBack);
        return callBack.getResult().replaceAll("\r|\n", "");
    }

    private String getRuleVal(String key, String defaultVal) {
        String v = rule.optString(key);
        if (v.isEmpty() || v.equals("空"))
            return defaultVal;
        return v;
    }

    private String getRuleVal(String key) {
        return getRuleVal(key, "");
    }

    private ArrayList<String> subContent(String content, String startFlag, String endFlag) {
        return Misc.subContent(content,startFlag,endFlag);
    }
}