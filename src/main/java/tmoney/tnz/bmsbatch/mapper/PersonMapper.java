package tmoney.tnz.bmsbatch.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import tmoney.tnz.bmsbatch.domain.Person;

import java.util.List;

@Mapper
public interface PersonMapper {
    @Insert("INSERT INTO PERSON (ID, NAME, DATE) VALUES (#{id}, #{name}, #{date})")
    void insert(Person person);
    // ADDED for Test
    @Select("SELECT * FROM PERSON")
    List<Person> findAll();
    // ADDED: 모든 데이터를 삭제하는 메소드 추가
    @Delete("DELETE FROM PERSON")
    void deleteAll();
}
