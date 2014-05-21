package com.soffid.iam.addons.backup.interceptor;

import java.lang.reflect.Method;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.SessionFactory;

import com.soffid.iam.addons.backup.service.UserBackupService;

import es.caib.seycon.ng.model.TasqueEntity;
import es.caib.seycon.ng.sync.engine.TaskHandler;

public class BackupInterceptor implements Interceptor, MethodInterceptor
{
	private SessionFactory sessionFactory = null;
	private UserBackupService userBackupService = null;



	public UserBackupService getUserBackupService() {
		return userBackupService;
	}

	public void setUserBackupService(UserBackupService userBackupservice) {
		this.userBackupService = userBackupservice;
	}

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public Object invoke(MethodInvocation mi) throws Throwable {
		Object result = mi.proceed();
		Method method = mi.getMethod();
		if (method.getParameterTypes().length == 1 &&
			! (mi.getArguments()[0] instanceof Long))
		{
			Object param = mi.getArguments()[0];
			if (method.getName().equals("create") && param instanceof TasqueEntity)
			{
				TasqueEntity tasque = (TasqueEntity) param;
				if (tasque.getTransa().equals(TaskHandler.UPDATE_USER))
				{
					userBackupService.performBackup(tasque.getUsuari());
				}
			}
		}
		return result;
	}
}
