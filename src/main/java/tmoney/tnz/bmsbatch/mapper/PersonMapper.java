package tmoney.tnz.bmsbatch.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import tmoney.tnz.bmsbatch.domain.Person;

@Mapper
public interface PersonMapper {
    @Insert("INSERT INTO PERSON (ID, NAME, DATE) VALUES (#{id}, #{name}, #{date})")
    void insert(Person person);
}
