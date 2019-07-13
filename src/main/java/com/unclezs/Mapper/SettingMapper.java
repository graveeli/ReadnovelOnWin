package com.unclezs.Mapper;

import com.unclezs.Model.DownloadConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/*
 *设置mapper
 *@author unclezs.com
 *@date 2019.07.07 10:36
 */
@Mapper
public interface SettingMapper {
    @Update("update setting set path=#{path},perThreadDownNum=#{perThreadDownNum},sleepTime=#{sleepTime},mergeFile=#{mergeFile} where id=1")
    void updateSetting(DownloadConfig config);

    @Select("select * from setting")
    DownloadConfig querySetting();

}
