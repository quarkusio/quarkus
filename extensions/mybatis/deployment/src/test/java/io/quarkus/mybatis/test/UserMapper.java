package io.quarkus.mybatis.test;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select * from users where id = #{id}")
    User getUser(Integer id);
}
