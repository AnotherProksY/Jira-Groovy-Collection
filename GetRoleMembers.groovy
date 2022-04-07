/* Пакет с кастомной JQL функцией. */


package com.onresolve.jira.groovy.jql

import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.util.MessageSet
import com.atlassian.jira.util.MessageSetImpl
import com.atlassian.jira.JiraDataType
import com.atlassian.jira.JiraDataTypes
import com.atlassian.jira.jql.operand.QueryLiteral
import com.atlassian.jira.jql.query.QueryCreationContext
import com.atlassian.query.clause.TerminalClause
import com.atlassian.query.operand.FunctionOperand
import com.onresolve.jira.groovy.jql.AbstractScriptedJqlFunction
import com.onresolve.scriptrunner.runner.ScriptRunnerImpl
import org.apache.log4j.Logger


class GetRoleMembers extends AbstractScriptedJqlFunction implements JqlFunction {

    def projectRoleManager = ComponentAccessor
        .getComponent(ProjectRoleManager)

    def projectManager = ComponentAccessor
        .getComponent(ProjectManager)

    @Override
    String getDescription() {
        "Check if user in specified project role"
    }

    @Override
    List<Map> getArguments() {
        [
            [ description: "Project role", optional: false ],
            [ description: "Project key" , optional: false ]
        ]
    }

    @Override
    String getFunctionName() {
        "roleMembers"
    }

    @Override
    JiraDataType getDataType() {
        JiraDataTypes.USER
    }

    @Override
    MessageSet validate(
        ApplicationUser user, FunctionOperand operand, TerminalClause terminalClause) {

        MessageSet messages = new MessageSetImpl()
        def role = projectRoleManager.getProjectRole(operand.args.get(0))
        def project = projectManager.getProjectObjByKey(operand.args.get(1))

        if (!role) {messages.addErrorMessage ("Project role '${operand.args.get(0)}' not found")}

        if (!project) {messages.addErrorMessage ("Project '${operand.args.get(1)}' not found")}

        return messages
    }

    @Override
    List<QueryLiteral> getValues(
        QueryCreationContext queryCreationContext, FunctionOperand operand, TerminalClause terminalClause) {

        def log = Logger.getLogger("groovy.errorlog.class")

        def role = projectRoleManager
            .getProjectRole(operand.args.get(0))

        def project = projectManager
            .getProjectObjByKey(operand.args.get(1))

        try {
            List<QueryLiteral> out = []

            def usersInRole = projectRoleManager
                .getProjectRoleActors(role, project)
                .getApplicationUsers().toList()

            for (user in usersInRole){
                out.add(new QueryLiteral(operand, user.name))
            }

            return out

        } catch (Exception error) {
            log.error(error.getMessage().toString())
        }
    }
}
