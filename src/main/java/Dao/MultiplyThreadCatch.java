package Dao;

//线程池版本抓取数据
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.NlpAnalysis;


import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiplyThreadCatch {
    private static class Job implements Runnable {
        private String url;
        private DataSource dataSource;
        private CountDownLatch countDownLatch;

        public Job(String url,DataSource dataSource,CountDownLatch countDownLatch) {
            this.url = url;
            this.dataSource = dataSource;
            this.countDownLatch=countDownLatch;
        }

        @Override
        public void run() {

            WebClient webClient = new WebClient(BrowserVersion.CHROME);
            webClient.getOptions().setJavaScriptEnabled(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setUseInsecureSSL(true);

            try {
                //计算SHA-256(因为MessageDigest 也不是线程安全的
                //所以，将其放入每个线程中去，每个线程独有自己的messageDigest )
                MessageDigest messageDigest=MessageDigest.getInstance("SHA-256");
                HtmlPage page = webClient.getPage(url);
                String xpath;
                DomText domText;
                //标题
                xpath = "//div[@class='cont']/h1/text()";
                domText = (DomText) page.getBody().getByXPath(xpath).get(0);
                String title = domText.asText();
                //朝代
                xpath = "//div[@class='cont']/p[@class='source']/a[1]/text()";
                domText = (DomText) page.getBody().getByXPath(xpath).get(0);
                String dynasty = domText.asText();
                //作者
                xpath = "//div[@class='cont']/p[@class='source']/a[2]/text()";
                domText = (DomText) page.getBody().getByXPath(xpath).get(0);
                String author = domText.asText();
                //正文
                xpath = "//div[@class='cont']/div[@class='contson']";
                HtmlElement element = (HtmlElement) page.getBody().getByXPath(xpath).get(0);
                String content = element.getTextContent();

                //计算sha-256
                String s = title + content;
                messageDigest.update(s.getBytes("UTF-8"));
                byte[] result = messageDigest.digest();
                StringBuilder sha256 = new StringBuilder();
                for (byte b : result) {
                    sha256.append(String.format("%02x", b));
                }
                //计算分词
                List<Term> termList = new ArrayList<>();
                termList.addAll(NlpAnalysis.parse(title).getTerms());
                termList.addAll(NlpAnalysis.parse(content).getTerms());
                List<String> words = new ArrayList<>();
                for (Term term : termList) {
                    if (term.getNatureStr().equals("w")) {
                        continue;
                    }
                    if (term.getNatureStr().equals("null")) {
                        continue;
                    }
                    if (term.getRealName().length() < 2) {
                        continue;
                    }
                    words.add(term.getRealName());
                }
                String insertwords = String.join(",", words);

                //保存数据库
                try (Connection connection = dataSource.getConnection()) {
                    String sql="INSERT INTO tang"+
                            "(sha256,dynasty,title,author,content,words)"+
                            "VALUES(?,?,?,?,?,?)";
                    try (PreparedStatement statement=connection.prepareStatement(sql)){
                        statement.setString(1,sha256.toString());
                        statement.setString(2,dynasty);
                        statement.setString(3,title);
                        statement.setString(4,author);
                        statement.setString(5,content);
                        statement.setString(6,insertwords);

                        statement.executeUpdate();
                        System.out.println("《"+title+"》"+"插入成功！");
                    }
                }
            } catch (SQLException e) {
                if(!e.getMessage().contains("Duplicate entry")){
                    e.printStackTrace();
                }
            }catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } finally {
                countDownLatch.countDown();
            }
        }
    }
    public static void main(String[] args)throws Exception {
        //执行前：当前时间
        long startTime=System.currentTimeMillis();

        WebClient client=new WebClient(BrowserVersion.CHROME);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);
        //列表页url
        String baseUrl="https://so.gushiwen.cn";
        String pathUrl="/gushi/tangshi.aspx";

        List<String> detailUrlList=new ArrayList<>();
        //列表页请求 + 解析
        try {
            String url = baseUrl + pathUrl;
            HtmlPage page = client.getPage(url);//Unhandled exceptions: java.io.IOException,

            List<HtmlElement> divs = page.getBody().getElementsByAttribute(
                    "div",
                    "class",
                    "typecont");
            for(HtmlElement div:divs){
                List<HtmlElement> as=div.getElementsByTagName("a");
                for(HtmlElement a:as){
                    String detailUrl=a.getAttribute("href");
                    detailUrlList.add(baseUrl+detailUrl);
                }
            }
        }catch (MalformedURLException e){
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(detailUrlList.size());
        //JDBC连接数据库
        MysqlConnectionPoolDataSource dataSource=new MysqlConnectionPoolDataSource();
        dataSource.setServerName("127.0.0.1");
        dataSource.setPort(3306);
        dataSource.setUser("root");
        dataSource.setPassword("");
        dataSource.setDatabaseName("2020_tangshi");
        dataSource.setUseSSL(true);
        dataSource.setCharacterEncoding("UTF8");

        ExecutorService pool=Executors.newFixedThreadPool(20);
        CountDownLatch countDownLatch=new CountDownLatch(detailUrlList.size());

        //详情页请求 + 请求
        for(String url:detailUrlList){
            pool.execute(new Job(url,dataSource,countDownLatch));
        }
        //等待所有诗都下载结束
        countDownLatch.await();//等待全部线程结束
        pool.shutdown();//关闭线程池
        //执行完：当前时间
        long endTime=System.currentTimeMillis();
        System.out.println((endTime-startTime)/1000);  //结果是以秒为单位
    }
}

