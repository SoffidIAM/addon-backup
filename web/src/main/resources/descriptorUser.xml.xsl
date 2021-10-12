<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" omit-xml-declaration="no" indent="yes"/>

	<xsl:template match="datanode[@name='usuari']" priority="3">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />

			<finder name="backups" type="backup" refreshAfterCommit="true" > 
				<ejb-finder jndi="openejb:/local/soffid.ejb.com.soffid.iam.addons.backup.service.UserBackupService"
					method="getUserBackups"
					if="${{canQueryBackup}}">
					<parameter value="${{instance.id}}" />
				</ejb-finder>
			</finder>

		</xsl:copy>
	</xsl:template>


	<xsl:template match="datanode[@name='root']" priority="3">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />

			<finder name="groupHistory" type="grup"> <!-- user:query -->
				<ejb-finder jndi="openejb:/local/soffid.ejb.com.soffid.iam.service.GroupService"
					method="findUserGroupHistoryByUserName" if="${{backupUser != null}}">
					<parameter value="${{backupUser}}" />
				</ejb-finder>
				<new-instance-bean className="es.caib.seycon.ng.comu.UsuariGrup"> 
				</new-instance-bean>
			</finder>

			<finder name="roleHistory" type="rol"> <!-- user:query -->
				<ejb-finder jndi="openejb:/local/soffid.ejb.com.soffid.iam.service.ApplicationService"
					method="findUserRolesHistoryByUserName" if="${{backupUser != null}}">
					<parameter value="${{backupUser}}" />
				</ejb-finder>
			</finder>

			<finder name="accountHistory" type="accountHistory"> <!-- user:query -->
				<ejb-finder jndi="openejb:/local/soffid.ejb.com.soffid.iam.service.AccountService"
					method="findSharedAccountsHistoryByUser" if="${{backupUser != null}}">
					<parameter value="${{backupUser}}" />
				</ejb-finder>
			</finder>

			<finder name="mailHistory" type="accountHistory"> <!-- user:query -->
				<ejb-finder jndi="openejb:/local/soffid.ejb.com.soffid.iam.service.MailListsService"
					method="findUserMailListHistoryByUserName" if="${{backupUser != null}}">
					<parameter value="${{backupUser}}" />
				</ejb-finder>
			</finder>

		</xsl:copy>
	</xsl:template>


	<xsl:template match="/zkib-model" priority="3">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		
			<datanode name="backup">
				<custom-attribute name="checked">
					return false;
				</custom-attribute>
				<finder name="text" type="backupText" refreshAfterCommit="false">
					<ejb-finder jndi="openejb:/local/soffid.ejb.com.soffid.iam.addons.backup.service.UserBackupService"
						method="getBackupContent">
						<parameter value="${{instance}}" />
					</ejb-finder>
				</finder>
			</datanode>
			
			<datanode name="backupText"/>
			
			<datanode name="accountHistory" transient="true"/>

		</xsl:copy>
	</xsl:template>


	<xsl:template match="node()|@*" priority="2">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>


</xsl:stylesheet>