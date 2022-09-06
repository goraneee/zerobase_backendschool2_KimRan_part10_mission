package zerobase.weather.repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import zerobase.weather.domain.Memo;
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;
@Repository
public class JdbcMemoRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public JdbcMemoRepository(DataSource dataSource) {   // properties에 저장한 정보들이 담긴다.
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // 데이터베이스에 저장
    public Memo save(Memo memo){
        String sql = " INSERT INTO memo values(?, ?) ; ";    // 데이터베이스에 메모 클래스를 넣어준다.
        jdbcTemplate.update(sql, memo.getId(), memo.getText());
        return memo;
    }

    // 모든 메모를 조회한다.
    private List<Memo> findAll(){
        String sql = " SELECT * FROM memo ;";
        return jdbcTemplate.query(sql, memoRowMapper());   // sql을 입력해서 나온 모든 객체가 반환된다. 데이터를 memoRowMapper()로 반환한다.
    }

    private Optional<Memo> findById(int id){
        String sql = " SELECT * " +
                     " FROM memo " +
                     " WHERE id = ?  ; ";

        return  jdbcTemplate.query(sql, memoRowMapper(), id).stream().findFirst();
    }

    // 데이터베이스에서 조회
    private RowMapper<Memo> memoRowMapper(){    // 데이터베이스에서 가져온 rs를 스프링 부트에 메모 클래스 형태로 반환한다.

        return  (rs, rowNum) -> new Memo(       // rs: resultSet
                rs.getInt("id"),
                rs.getString("text")
        );
    }
}
