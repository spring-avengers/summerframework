package com.bkjk.platform.mybatis.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 1：批量相关接口 2：查询分页使用Map作为入参
 **/
public interface MyBaseMapper<T> extends com.baomidou.mybatisplus.core.mapper.BaseMapper<T> {

    /**
     * <p>
     * 插入（批量）
     * </p>
     * 
     * @param entityList 实体对象列表
     * @return int
     */
    int insertBatch(List<T> entityList);

    /**
     * <p>
     * 插入，若已经存在则更新。只支持MySql
     * </p>
     *
     * @param entity 实体
     * @return int
     */
    int saveOrUpdate(T entity);

    /**
     * <p>
     * 根据ID 批量更新
     * </p>
     * 
     * @param entityList 实体对象列表
     * @return int
     */
    int updateBatchById(List<T> entityList);

    /**
     * 精确分页查询
     * 
     * @param page：分页查询对象
     * @param param: key须是数据库字段，value是比较值
     * @return
     */
    default Page<T> selectPageAccurate(Page<T> page, Map<String, Object> param) {
        return selectPage(page, Wrappers.<T>query().allEq(param));
    }

    /**
     * 精确总数查询
     * 
     * @param param: key须是数据库字段，value是比较值
     * @return
     */
    default Integer selectCountAccurate(Map<String, Object> param) {
        return selectCount(Wrappers.<T>query().allEq(param));
    }

    /**
     * 模糊分页查询
     * 
     * @param page：分页查询对象
     * @param param: key须是数据库字段，value是比较值
     * @return
     */
    default Page<T> selectPageBlurry(Page<T> page, Map<String, Object> param) {
        QueryWrapper<T> query = Wrappers.<T>query();
        if (null != param) {
            param.forEach((k, v) -> {
                query.like(k, v);
            });
        }
        return selectPage(page, query);
    }

    /**
     * 模糊总数查询
     * 
     * @param param: key须是数据库字段，value是比较值
     * @return
     */
    default Integer selectCountBlurry(Map<String, Object> param) {
        QueryWrapper<T> query = Wrappers.<T>query();
        if (null != param) {
            param.forEach((k, v) -> {
                query.like(k, v);
            });
        }
        return selectCount(query);
    }

    /**
     * 内部使用方法 强转Ipage对象
     * 
     * @param page
     * @param queryWrapper
     * @return
     */
    default Page<T> selectPage(Page<T> page, @Param(Constants.WRAPPER) Wrapper<T> queryWrapper) {
        return (Page<T>)selectPage((IPage<T>)page, queryWrapper);
    }

}
