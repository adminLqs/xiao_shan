package com.xiaoshan.springbootdemo.mapper;

import com.xiaoshan.springbootdemo.entity.UserProfile;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * 用户资料数据访问接口
 * 负责用户资料表的 CRUD 操作
 *
 * @author your-team
 * @since 2026-01-15
 */
@Mapper
public interface UserProfileMapper {

    // ==================== 增（Create） ====================

    /**
     * 插入用户资料
     *
     * @param userProfile 用户资料对象
     * @return 影响行数
     */
    @Insert("INSERT INTO user_profiles (user_id, avatar, nickname, gender, birthday, region, bio, phone, email, email_verified) " +
            "VALUES (#{userId}, #{avatar}, #{nickname}, #{gender}, #{birthday}, #{region}, #{bio}, #{phone}, #{email}, #{emailVerified})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserProfile userProfile);

    // ==================== 删（Delete） ====================

    /**
     * 根据主键ID删除用户资料
     *
     * @param id 主键ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_profiles WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据用户ID删除用户资料
     *
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_profiles WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    // ==================== 改（Update） ====================

    /**
     * 更新用户资料（根据主键ID）
     *
     * @param userProfile 用户资料对象
     * @return 影响行数
     */
    @Update("UPDATE user_profiles SET avatar = #{avatar}, nickname = #{nickname}, gender = #{gender}, " +
            "birthday = #{birthday}, region = #{region}, bio = #{bio}, phone = #{phone}, " +
            "email = #{email}, email_verified = #{emailVerified} WHERE id = #{id}")
    int updateById(UserProfile userProfile);

    /**
     * 根据用户ID更新用户资料
     *
     * @param userProfile 用户资料对象
     * @return 影响行数
     */
    @Update("UPDATE user_profiles SET avatar = #{avatar}, nickname = #{nickname}, gender = #{gender}, " +
            "birthday = #{birthday}, region = #{region}, bio = #{bio}, phone = #{phone}, " +
            "email = #{email}, email_verified = #{emailVerified} WHERE user_id = #{userId}")
    int updateByUserId(UserProfile userProfile);

    // ==================== 查（Select） ====================

    /**
     * 根据主键ID查询资料
     *
     * @param id 主键ID
     * @return 用户资料（可能为空）
     */
    @Select("SELECT id, user_id, avatar, nickname, gender, birthday, region, bio, phone, email, email_verified " +
            "FROM user_profiles WHERE id = #{id}")
    Optional<UserProfile> findById(Long id);

    /**
     * 根据用户ID查询资料
     *
     * @param userId 用户ID
     * @return 用户资料（可能为空）
     */
    @Select("SELECT id, user_id, avatar, nickname, gender, birthday, region, bio, phone, email, email_verified " +
            "FROM user_profiles WHERE user_id = #{userId}")
    Optional<UserProfile> findByUserId(Long userId);

    /**
     * 批量根据用户ID查询用户资料
     * 用于评论列表批量获取用户头像、昵称，避免 N+1 查询问题
     *
     * @param userIds 用户ID列表
     * @return 用户资料列表
     */
    @Select("<script>" +
            "SELECT id, user_id, avatar, nickname, gender, birthday, region, bio, phone, email, email_verified " +
            "FROM user_profiles WHERE user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<UserProfile> findByUserIds(@Param("userIds") List<Long> userIds);

    /**
     * 检查用户资料是否存在（根据用户ID）
     *
     * @param userId 用户ID
     * @return true-存在，false-不存在
     */
    @Select("SELECT COUNT(*) > 0 FROM user_profiles WHERE user_id = #{userId}")
    boolean existsByUserId(Long userId);
}