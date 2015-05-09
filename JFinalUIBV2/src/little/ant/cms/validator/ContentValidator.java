package little.ant.cms.validator;

import org.apache.log4j.Logger;

import com.jfinal.core.Controller;
import com.jfinal.validate.Validator;
import little.ant.cms.model.Content;

public class ContentValidator extends Validator {

	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(ContentValidator.class);
	
	protected void validate(Controller controller) {
		String actionKey = getActionKey();
		if (actionKey.equals("/jf/cms/content/save")){
			// validateString("username", 6, 30, "usernameMsg", "请输入登录账号!");
			
		} else if (actionKey.equals("/jf/cms/content/update")){
			
		}
	}
	
	protected void handleError(Controller controller) {
		controller.keepModel(Content.class);
		
		String actionKey = getActionKey();
		if (actionKey.equals("/jf/cms/content/save")){
			controller.render("/cms/xxx.html");
		
		} else if (actionKey.equals("/jf/cms/content/update")){
			controller.render("/cms/xxx.html");
		
		}
	}
	
}