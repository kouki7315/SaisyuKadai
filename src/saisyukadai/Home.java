package saisyukadai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class Home {
    public static void main(String[] args) throws TwitterException{
        Twitter twitter = new TwitterFactory().getInstance();
        Query query = new Query();

        // 検索ワードの設定
        query.setQuery("ニンテンドースイッチ");

        // 1度のリクエストで取得するTweetの数
        query.setCount(20);

        QueryResult result = null;
        try {
            result = twitter.search(query);
        } catch (TwitterException e) {
            e.printStackTrace();
        }
        Iterator itr = result.getTweets().iterator();
        String senti = "positive";

        while (itr.hasNext()) {
            Status status = (Status)itr.next();
            String text = status.getText();
            Sentiment(text,senti);
            if (!senti.equals("negative")) {
                System.out.println(text); // 本文
                System.out.println(status.getUser().getName()); // ユーザー名
                System.out.println(status.getCreatedAt()); // 日付
                System.out.println( );
            }
        }
    }


    public static void Sentiment (String text,String senti){

        String urlstr = "https://api.apitore.com/api/11/sentiment/predict?access_token=5898cbe7-3c24-4624-b5d6-ae7850999728&text=";
        HttpURLConnection con = null;
        String response = null;

        try{
            String encodedtext = URLEncoder.encode(text, "UTF-8");
            urlstr = urlstr + encodedtext;
            URL url = new URL(urlstr);
            con = (HttpURLConnection) url.openConnection();
            con.connect();

            try ( InputStream in = con.getInputStream();
                    InputStreamReader inReader = new InputStreamReader(in, "UTF-8");
                    BufferedReader bufReader = new BufferedReader(inReader)) {
                  String line = null;
                  while((line = bufReader.readLine()) != null) {
                      //System.out.println(line);
                      response = line;
                  }
                  bufReader.close();
                  inReader.close();
                  in.close();
              } catch (IOException e ){
              }

        }catch (MalformedURLException e1) {
            e1.printStackTrace();
        }catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }catch (ProtocolException e1) {
            e1.printStackTrace();
        }catch (IOException e1){
            e1.printStackTrace();
        }finally{
            if(con != null){
                con.disconnect();
            }
        }

        senti = response.substring(response.indexOf("sentiment") + 12,response.indexOf("sentiment") + 20);

        if (senti.contains("\""))
        {
            senti = senti.replaceAll("\"", " ");
        }
        //System.out.println(senti);
    }
}

