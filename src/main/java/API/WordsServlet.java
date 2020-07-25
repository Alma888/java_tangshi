package API;

import com.alibaba.fastjson.JSONArray;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @ClassName WordsServlet
 * @Description TODO
 * @Auther liujing
 * @Date 2020/7/21 12:07]
 * @Version 1.0
 **/



public class WordsServlet extends HttpServlet {
    class tangshi{
        String title;
        int count;

        public tangshi(String title, int count) {
            this.title = title;
            this.count = count;
        }
    }
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String count=req.getParameter("count");
        if(count==null){
            count="5";
        }
        Connection connection=null;
        PreparedStatement statement=null;
        ResultSet set=null;
        Map<String,Integer> map=new HashMap<>();
        try {
            connection=DBConfig.getConnection();
            String sql01="select words from tang";
            statement=connection.prepareStatement(sql01);
            set=statement.executeQuery();
            while(set.next()){
                String word=set.getString("words");
                String[] words=word.split(" ");
                for(String ss:words){
                    map.put(ss,map.getOrDefault(ss,0)+1);
                }
            }
            List<tangshi> list=new ArrayList<>();
            for(String ss:map.keySet()){
                list.add(new tangshi(ss,map.get(ss)));
            }
            Collections.sort(list, new Comparator<tangshi>() {
                @Override
                public int compare(tangshi o1, tangshi o2) {
                    return o2.count-o1.count;
                }
            });
            JSONArray array=new JSONArray();
            Integer num=Integer.valueOf(count);
            for(tangshi t:list){
                JSONArray item=new JSONArray();
                item.add(t.title);
                item.add(map.get(t.title));
                array.add(item);
                num--;
                if(num==0){
                    break;
                }
            }
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().write(array.toJSONString());
        }catch (SQLException e) {
            e.printStackTrace();
            resp.setContentType("text/html;charSet=utf-8");
            resp.getWriter().write("error");
        }finally {
            DBConfig.close(connection,statement,set);
        }
    }
}
