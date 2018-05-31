package saisyukadai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.atilika.kuromoji.Token;
import org.atilika.kuromoji.Tokenizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import twitter4j.TwitterException;

public class SentimentSearch {
    public static void main(String[] args) throws TwitterException, InterruptedException, IOException{
        Path SentiSearch = Paths.get("C:/TechTraining/resources/SentimentSearch.csv");
        Files.deleteIfExists(SentiSearch); // 既に存在してたら削除
        Files.createFile(SentiSearch); // ファイル作成

        //ツイート検索
        String rootUrl = "https://twitter.com/";
        String queryWord = "ポケモン";
        String since = "2018-05-30"; // いつから
        String until = "2018-05-31"; // いつまで
        String queryUrl = rootUrl + "search?f=tweets&vertical=news&q="
                         + queryWord + "%20since%3A" + since + "%20until%3A" + until + "&src=typd";
        Document doc = Jsoup.connect(queryUrl).get();
        String min_position = doc.select("div[data-max-position]").get(0).attr("data-max-position");
        //System.out.println(min_position);

        try (BufferedWriter bw = Files.newBufferedWriter(SentiSearch)) {
            bw.write("ユーザー名,日付,感情,本文");
            bw.newLine();

            for(int i =0; i<1000 ; i++) {
                try {
                    int page = i+1;
                    System.out.println("\n=========="+ page +"ページ目=========");

                    Element tab = doc.select("div.stream > ol").get(0);

                    for (Element tweet : tab.children()) {
                        if (tweet.children().size() < 1) {
                            continue;
                        }
                        String text = tweet.select("p").get(0).text();
                        text = text.replaceAll(",", "，");
                        String senti = Sentiment(text);
                        long dateTime = Long.parseLong(tweet.select("small.time > a >span").get(0).attr("data-time-ms"));
                        String date = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format( dateTime );
                        String name = tweet.select("span.FullNameGroup > strong").get(0).text();
                        name = name.replaceAll(",", "，"); // ,は全角に
                        text = text.replaceAll("\r", " ").replaceAll("\n", " ").replaceAll("\r/n", " "); // 改行を消す
                        System.out.println(name + "," + date + "," + senti + "," +text +"\n");
                        String str = name + "," + date + "," + senti + "," +text;
                        bw.write(str);
                        bw.newLine();
                    }

                    min_position = doc.select("div[data-min-position]").get(0).attr("data-min-position");
                    //System.out.println(min_position);
                    Thread.sleep(1000);
                    doc = Jsoup.connect(queryUrl)
                            .data("max_position",min_position)
                            .get();
                }catch(IndexOutOfBoundsException e) {
                    break;
                }
            }
            System.out.println("終了しました！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String Sentiment (String text) throws IOException{
        // ネガポジ判定のための辞書の処理
        Path verbsSentimentDictionary = Paths.get("C:/TechTraining/resources/wago.121808.pn");
        Path nounsSentimentDictionary = Paths.get("C:/TechTraining/resources/pn.csv.m3.120408.trim");
        Map<String,Integer> verbsMap = new HashMap<>(); // 動詞,得点
        Map<String,Integer> nounsMap = new HashMap<>(); // 名詞,得点

        //用言を辞書からmapへ
        try (BufferedReader br = Files.newBufferedReader(verbsSentimentDictionary)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("\\t", ",");
                String[] elem = line.split(",");
                String verb;
                try {
                    verb = elem[1];
                } catch (ArrayIndexOutOfBoundsException e) {
                    verb = " ";
                }
                int point = 0;
                if( elem[0].contains("ネガ") ) {
                    point = -1;
                } else if( elem[0].contains("ポジ") ) {
                    point = 1;
                }
                //System.out.println(verb +"," + point);
                verbsMap.put(verb, point);
            }
        }

        //名詞を辞書からmapへ
        try (BufferedReader br = Files.newBufferedReader(nounsSentimentDictionary)) {
            String line;
            line = br.readLine();
            while ((line = br.readLine()) != null) {
                line = line.replaceAll("\\t", ",");
                String[] elem = line.split(",");
                String noun;
                try {
                    noun = elem[0];
                } catch (ArrayIndexOutOfBoundsException e) {
                    noun = " ";
                }
                int point = 0;
                if( elem[1].equals("n") ) {
                    point = -1;
                    nounsMap.put(noun, point);
                } else if( elem[1].equals("p") ) {
                    point = 1;
                    nounsMap.put(noun, point);
                }
                //System.out.println(noun +"," + point);
            }
        }

        Tokenizer tokenizer = Tokenizer.builder().build();
        int count =0;
        double point = 0;
        for (Token token : tokenizer.tokenize(text)) {
          //System.out.println(token.getSurfaceForm() + " " + token.getAllFeatures());
          String[] elem = token.getAllFeaturesArray();
          String word = elem[6];
          String wordClass = elem[0];

          String[] str = {"動詞","形容詞","形容動詞"};
          List<String> list = Arrays.asList(str);
          try {
              if( list.contains(wordClass) ) {
                  point = point + verbsMap.get(word);
                  count ++;
                  System.out.print(elem[6] + "," + elem[0]);
                  System.out.println(point + "," +count);
              }else if ( wordClass.equals("名詞") ) {
                  point = point + nounsMap.get(word);
                  count ++;
                  System.out.print(elem[6] + "," + elem[0]);
                  System.out.println(point + "," +count);
              }
          } catch(NullPointerException e) {
          }

        }
        double score =0;
        if(!(count==0)) {
            score = point/count;
        }
        String senti = null;
        if (score>0) {
            senti = "positive";
        } else if(score==0) {
            senti = "neutral";
        } else if(score<0){
            senti = "negative";
        }
        System.out.println("score:" + score);
        System.out.println("sentiment:" + senti);
        return senti;
    }
}

