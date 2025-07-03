package tmoney.gbi.bmsbatch.mapper;

import org.apache.ibatis.annotations.Mapper;
import tmoney.gbi.bmsbatch.domain.SamplePersonDto;

import java.util.List;

@Mapper
public interface SampleMapper {

    void insert(SamplePersonDto person);
    // ADDED for Test

    List<SamplePersonDto> findAll();
    // ADDED: 모든 데이터를 삭제하는 메소드 추가

    void deleteAll();
}
