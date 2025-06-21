package tmoney.tnz.bmsbatch.mapper;

import org.apache.ibatis.annotations.Mapper;
import tmoney.tnz.bmsbatch.domain.Person;

import java.util.List;

@Mapper
public interface PersonMapper {

    void insert(Person person);
    // ADDED for Test

    List<Person> findAll();
    // ADDED: 모든 데이터를 삭제하는 메소드 추가

    void deleteAll();
}
