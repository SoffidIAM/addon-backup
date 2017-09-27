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
			
		</xsl:copy>
	</xsl:template>


	<xsl:template match="node()|@*" priority="2">
		<xsl:copy>
			<xsl:apply-templates select="node()|@*" />
		</xsl:copy>
	</xsl:template>


</xsl:stylesheet>