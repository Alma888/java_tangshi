package Dao;

//单线程版本抓取数据
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.sql.Connection;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.NlpAnalysis;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class SingleThreadCatch {
    public static void main(String[] args) throws Exception{
        //执行前：当前时间
        long startTime=System.currentTimeMillis();


        WebClient client=new WebClient(BrowserVersion.CHROME);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setCssEnabled(false);
        client.getOptions().setUseInsecureSSL(true);

        String baseUrl="https://so.gushiwen.cn";
        String pathUrl="/gushi/tangshi.aspx";
        List<String> detailUrlList=new ArrayList<>();

        //列表页请求 + 解析
        {
            String url=baseUrl+pathUrl;
            HtmlPage page=client.getPage(url);
            List<HtmlElement> divs=page.getBody().getElementsByAttribute(
                    "div",
                    "class",
                    "typecont"
            );

            for(HtmlElement div:divs){
                List<HtmlElement> as=div.getElementsByTagName("a");
                for(HtmlElement a:as){
                    String detailUrl=a.getAttribute("href");
                    detailUrlList.add(baseUrl+detailUrl);
                }
            }
        }

        MysqlConnectionPoolDataSource dataSource=new MysqlConnectionPoolDataSource();
        dataSource.setServerName("127.0.0.1");
        dataSource.setPort(3306);
        dataSource.setUser("root");
        dataSource.setPassword("");
        dataSource.setDatabaseName("2020_tangshi");
        dataSource.setUseSSL(true);//不用安全层
        dataSource.setCharacterEncoding("UTF8");

        Connection connection=dataSource.getConnection();
        String sql="INSERT INTO tang"+
                "(sha256,dynasty,title,author,content,words)"+
                "VALUES(?,?,?,?,?,?)";
        PreparedStatement statement=connection.prepareStatement(sql);

        MessageDigest messageDigest=MessageDigest.getInstance("SHA-256");

        //详情页请求 + 解析
        {
            for(String url:detailUrlList){
                HtmlPage page=client.getPage(url);
                String xpath;
                DomText domText;

                //标题
                xpath="//div[@class='cont']/h1/text()";
                domText=(DomText) page.getBody().getByXPath(xpath).get(0);
                String title=domText.asText();

                //朝代
                xpath="//div[@class='cont']/p[@class='source']/a[1]/text()";
                domText=(DomText) page.getBody().getByXPath(xpath).get(0);
                String dynasty=domText.asText();
                //作者
                xpath="//div[@class='cont']/p[@class='source']/a[2]/text()";
                domText=(DomText) page.getBody().getByXPath(xpath).get(0);
                String author=domText.asText();
                //正文
                xpath="//div[@class='cont']/div[@class='contson']";
                HtmlElement htmlElement=(HtmlElement)page.getBody().getByXPath(xpath).get(0);
                String content=htmlElement.getTextContent().trim();

                //1.计算sha-256
                String s=title+content;
                messageDigest.update(s.getBytes("utf-8"));
                byte[] result=messageDigest.digest();
                StringBuilder sha256=new StringBuilder();
                for(byte b:result){
                    sha256.append(String.format("%02x",b));
                }

                //2. 计算分词

                List<Term> termList=new ArrayList<>();
                termList.addAll(NlpAnalysis.parse(title).getTerms());
                termList.addAll(NlpAnalysis.parse(content).getTerms());
                List<String> words=new ArrayList<>();
                for(Term term:termList){
                    if(term.getNatureStr().equals("w")){
                        continue;
                    }
                    if(term.getNatureStr().equals("null")){
                        continue;
                    }
                    if(term.getRealName().length()<2){
                        continue;
                    }
                    words.add(term.getRealName());
                }
                //通过调用join()将所有的words就连接成一个完整字符串
                String insertwords=String.join(",",words);

                //插入数据库
                statement.setString(1,sha256.toString());
                statement.setString(2,dynasty);
                statement.setString(3,title);
                statement.setString(4,author);
                statement.setString(5,content);
                statement.setString(6,insertwords);

                statement.executeUpdate();
                System.out.println("《"+title+"》"+"插入成功");
            }
        }

        //执行完：当前时间
        long endTime=System.currentTimeMillis();
        System.out.println((endTime-startTime)/1000);  //结果是以秒为单位
        /*
        HtmlElement element=page.getBody();
        int count=0;
        List<HtmlElement> elementList=element.getElementsByAttribute("div",
                "class",
                "typecont");
        for(HtmlElement element1:elementList){
            List<HtmlElement> elements=element1.getElementsByTagName("a");
            for(HtmlElement e:elements){
                count++;
                System.out.println(e.getAttribute("href"));
            }
        }
        System.out.println(count);
        */
    }
}
