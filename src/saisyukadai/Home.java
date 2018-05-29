package saisyukadai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

public class Home {
    public static void main(String[] args) throws TwitterException, InterruptedException{
        Path answer = Paths.get("C:/TechTraining/resources/answer.csv");
        Twitter twitter = new TwitterFactory().getInstance();
        Query query = new Query();

        // 検索ワードの設定
        String word = "#ヨルナイト";
        word = word + " -rt";
        query.setQuery(word);

        // 1度のリクエストで取得するTweetの数（最大100件）
        query.setCount(100);
        query.resultType(Query.RECENT);
        QueryResult result = null;

        try (BufferedWriter bw = Files.newBufferedWriter(answer)) {
            bw.write("ユーザー名,日付,本文");
            bw.newLine();

            // 最大1500件（15ページ）
            for (int i = 1; i <= 1; i++) {
                try {
                    result = twitter.search(query);
                } catch (TwitterException e) {
                    e.printStackTrace();
                }
                Iterator<?> itr = result.getTweets().iterator();
                String senti = null;

                while (itr.hasNext()) {
                    Status status = (Status)itr.next();
                    String text = status.getText();
                    String senti2 = Sentiment(text,senti);
                    if (!senti2.equals("negative")) {
                        String name = status.getUser().getName();
                        String date = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(status.getCreatedAt());
                        text = text.replaceAll("\r", " ").replaceAll("\n", " ").replaceAll("\r/n", " ");
                        System.out.println(text); // 本文
                        System.out.println(name); // ユーザー名
                        System.out.println(date); // 日付
                        System.out.println( );
                        String str = name + "," + date + "," + text;
                        bw.write(str);
                        bw.newLine();
                    }
                }

                if (result.hasNext()) {
                    query = result.nextQuery();
                    Thread.sleep(1000);
                } else {
                    break;
                }
            }

            System.out.println("終了しました！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String Sentiment (String text,String senti){

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

        //System.out.println(response);
        senti = response.substring(response.indexOf("sentiment") + 12,response.indexOf("sentiment") + 20);

        if (senti.contains("\""))
        {
            senti = senti.replaceAll("\"", " ");
        }
        //System.out.println(senti);
        return senti;
    }
}

