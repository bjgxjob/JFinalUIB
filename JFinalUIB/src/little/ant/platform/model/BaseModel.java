package little.ant.platform.model;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import little.ant.platform.tools.ToolSqlXml;
import little.ant.platform.tools.ToolUtils;
import oracle.sql.TIMESTAMP;

import org.apache.log4j.Logger;

import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Table;
import com.jfinal.plugin.activerecord.TableMapping;

/**
 * Model基础类
 * 
 * @author 董华健
 * @param <M>
 */
public abstract class BaseModel<M extends Model<M>> extends Model<M> {

	private static final long serialVersionUID = -900378319414539856L;

	private static Logger log = Logger.getLogger(BaseModel.class);
	
	/**
	 * 根据i18n参数查询获取哪个字段的值
	 * @param i18n
	 * @return
	 */
	public static String i18n(String i18n){
		String val = "_zhcn";
		if(i18n.equals("zh") || i18n.equals("zh_cn")){
			val = "_zhcn";
			
		} else if(i18n.equals("en") || i18n.equals("en_us")){
			val = "_enus";
			
		} else if(i18n.equals("ja") || i18n.equals("ja_jp")){
			val = "_ja";
			
		} else if(i18n.equals("zh_hk")){
			val = "_zhhk";
			
		} else if(i18n.equals("zh_tw")){
			val = "_zhtw";
		}
		return val;
	}
	
	/**
	 * 获取表映射对象
	 * 
	 * @return
	 */
	public Table getTable() {
		return TableMapping.me().getTable(getClass());
	}

	/**
	 * 获取主键值
	 * @return
	 */
	public String getPrimaryKeyValue(){
		return this.getStr(getTable().getPrimaryKey());
	}

	/**
	 * 重写save方法
	 */
	public boolean save() {
		this.set(getTable().getPrimaryKey(), ToolUtils.getUuidByJdk(true)); // 设置主键值
		if(getTable().hasColumnLabel("version")){ // 是否需要乐观锁控制
			this.set("version", Long.valueOf(0)); // 初始化乐观锁版本号
		}
		return super.save();
	}

	/**
	 * 重写update方法
	 */
	@SuppressWarnings("unchecked")
	public boolean update() {
		Table table = getTable();
		String name = table.getName();
		String pk = table.getPrimaryKey();
		
		// 1.数据是否还存在
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("table", name);
		param.put("pk", pk);
		String sql = ToolSqlXml.getSql("platform.baseModel.version", param); 
		Model<M> modelOld = findFirst(sql , getStr("ids"));
		if(null == modelOld){ // 数据已经被删除
			throw new RuntimeException("数据库中此数据不存在，可能数据已经被删除，请刷新数据后在操作");
		}
		
		// 2.乐观锁控制
		Set<String> modifyFlag = null;
		try {
			Field field = this.getClass().getSuperclass().getSuperclass().getDeclaredField("modifyFlag");
			field.setAccessible(true);
			Object object = field.get(this);
			if(null != object){
				modifyFlag = (Set<String>) object;
			}
			field.setAccessible(false);
		} catch (NoSuchFieldException | SecurityException e) {
			log.error("业务Model类必须继承BaseModel");
			e.printStackTrace();
			throw new RuntimeException("业务Model类必须继承BaseModel");
		} catch (IllegalArgumentException | IllegalAccessException e) {
			log.error("BaseModel访问modifyFlag异常");
			e.printStackTrace();
			throw new RuntimeException("BaseModel访问modifyFlag异常");
		}
		boolean versionModify = modifyFlag.contains("version");
		if(versionModify && getTable().hasColumnLabel("version")){ // 是否需要乐观锁控制
			Long versionDB = modelOld.getNumber("version").longValue(); // 数据库中的版本号
			Long versionForm = getNumber("version").longValue(); // 表单中的版本号
			if(!(versionForm > versionDB)){
				throw new RuntimeException("表单数据版本号和数据库数据版本号不一致，可能数据已经被其他人修改，请重新编辑");
			}
		}
		
		return super.update();
	}

	/**
	 * 针对Oracle做特殊处理
	 * @param attr
	 * @return
	 */
	@Override
	public Date getDate(String attr) {
		Object obj = this.get(attr);
		if(null == obj){
			return null;
		}
		
		if (TIMESTAMP.class.isAssignableFrom(obj.getClass())){
			TIMESTAMP ts = (TIMESTAMP) obj;
			
			Date date = null;
			try {
				date = ts.timestampValue();
			} catch (SQLException e) {
				return null;
			}
			
			return date;
		}
		
		return (Date) obj;
	}
	
}