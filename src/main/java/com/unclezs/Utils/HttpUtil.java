package com.unclezs.Utils;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/*
 *@author unclezs.com
 *@date 2019.06.20 23:27
 */
public class HttpUtil {
    //get/post请求静态网页
    public static String request(String url){
        try(CloseableHttpClient client= HttpClients.createDefault()){
            HttpEntity entity = client.execute(new HttpGet(url)).getEntity();
            String responce = EntityUtils.toString(entity, "UTF-8");
            return responce;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    public static int getResponseCode(String url){
        try {
            HttpURLConnection connection=(HttpURLConnection) new URL(url).openConnection();
            connection.setRequestProperty("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/73.0.3683.103 Safari/537.36");
            return connection.getResponseCode();
        }catch (Exception e){
            return -1;
        }
    }

    /**
     * post请求
     * @param url
     * @param data 数据
     * @return
     */
    public static String doPost(String url, List<NameValuePair> data){
       return doPost(url,data,"UTF-8");
    }

    /**
     * post请求
     * @param url 地址
     * @param data 数据
     * @param charset 编码
     * @return
     */
    public static String doPost(String url, List<NameValuePair> data,String charset){
        HttpPost post=new HttpPost(url);
        //设置请求超时
        RequestConfig config=RequestConfig.custom()
                .setConnectTimeout(10000)
                .setSocketTimeout(10000)
                .setConnectionRequestTimeout(10000)
                .build();
        try(CloseableHttpClient client=HttpClients
                .custom()
                .setDefaultRequestConfig(config)
                .build()){
            UrlEncodedFormEntity entity=new UrlEncodedFormEntity(data,charset);
            post.setEntity(entity);
            HttpEntity httpEntity = client.execute(post).getEntity();
            return EntityUtils.toString(httpEntity,charset);
        } catch (Exception e) {
            System.out.println("请求超时"+url);
        }
        return "";
    }


    //获取流
    public static InputStream stream(String url){
        try(CloseableHttpClient client= HttpClients.createDefault()) {
            return client.execute(new HttpGet(url)).getEntity().getContent();
        }catch (Exception e){
            return null;
        }
    }
}
