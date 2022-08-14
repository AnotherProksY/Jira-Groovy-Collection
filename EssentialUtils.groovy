/* Пакет с набором полезных функций для JIRA. */


package com.custom

import java.lang.Character.UnicodeBlock;                                 // Language Java pack
import org.apache.log4j.Logger;                                          // Logger
import com.atlassian.jira.component.ComponentAccessor;                   // ComponentAccessor
import com.atlassian.jira.issue.Issue;                                   // Issue
import com.atlassian.jira.issue.comments.CommentManager;                 // Comments
import com.atlassian.jira.issue.IssueManager;                            // Issue Manager
import com.atlassian.jira.user.ApplicationUser;                          // Users
import com.atlassian.jira.bc.issue.search.SearchService;                 // Searching
import com.atlassian.jira.web.bean.PagerFilter;                          // PagerFilter
import com.atlassian.jira.issue.label.LabelManager;                      // LabelManager
import com.atlassian.jira.issue.label.Label;                             // Label
import com.atlassian.jira.issue.label.LabelParser;                       // LabelParser
import com.atlassian.jira.project.Project;                               // Project
import com.atlassian.jira.issue.MutableIssue;                            // Mutable Issue
import com.atlassian.jira.event.type.EventDispatchOption;                // EventDispatch
import com.atlassian.jira.issue.CustomFieldManager;                      // CustomField Manger
import com.atlassian.jira.issue.fields.CustomField;                      // CustomField
import com.atlassian.jira.security.groups.GroupManager;                  // GroupManager
import com.atlassian.jira.issue.fields.config.FieldConfig;               // Field Config
import com.atlassian.jira.issue.customfields.manager.OptionsManager;     // OptionsManager
import com.atlassian.jira.issue.customfields.option.Option;              // Option
import com.atlassian.jira.issue.customfields.option.Options;             // Options
import com.atlassian.jira.issue.ModifiedValue;                           // ModifiedValue
import com.atlassian.jira.issue.util.DefaultIssueChangeHolder;           // DefaultIssueChangeHolder


class EssentialUtils {

    /* Интерфейсы */
    private static final Logger logger = Logger.getLogger("groovy.errorlog.class");
    private static final IssueManager issueManager = ComponentAccessor.getIssueManager();
    private static final CommentManager comment = ComponentAccessor.getCommentManager();
    private static final LabelManager labelManager = ComponentAccessor.getComponent(LabelManager);
    private static final OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
    private static final ApplicationUser user = ComponentAccessor.getUserUtil().getUser(username);
    private static final ApplicationUser currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();

    /* Статика */
    private static final String username = "service_user";


    // -------- "ПОЛУЧАЕМ ОБЪЕКТ ТИКЕТА ПО ЕГО НОМЕРУ" ----------
    public static Issue getIssueObjByKey(String issueKey, Boolean logEvent=true) {

        try {
            // Получаем объект задачи по его номеру в JIRA
            Issue issueObject = issueManager.getIssueObject(issueKey); 

            // True -> возвращаем объект задачи
            // False -> возвращаем null
            if (issueObject) {
                if (logEvent) log("Issue Object was successfully obtained!");
                return issueObject;
            } else {
                if (logEvent) log("Issue Object wasn't found :(");
                return null;
            }
        } catch (Exception error) {
            log(error);
            return null;
        }

    }
    private static Issue getIssueObjById(Long issueId, Boolean logEvent=true) {

        try {
            // Получаем объект задачи по его Issue ID
            Issue issueObject = issueManager.getIssueObject(issueId); 

            // True -> возвращаем объект задачи
            // False -> возвращаем null
            if (issueObject) {
                if (logEvent) log("Issue Object was successfully obtained!");
                return issueObject;
            } else {
                if (logEvent) log("Issue Object wasn't found :(");
                return null;
            }
        } catch (Exception error) {
            log(error);
            return null;
        }

    }
    // ----------------------------------------------------------


    // ---------- "СОЗДАНИЕ КОММЕНТАРИЕВ В ТИКЕТЕ" --------------
    public static void newComment(Issue issue, String text, ApplicationUser author=null, String roleName=null, String groupName=null, Boolean notify=false) {

        // Если все ок, добавляем комментарий 
        // Если не ок, получаем исключение 
        try{
            // Получаем ID нужной проектной роли
            Long roleNameId
            if (roleNameId) { roleNameId = getRoleNameId(roleName) }

            comment.create(
                issue,
                author ? author : user,
                text,
                groupName ? groupName : null,
                roleNameId ? roleNameId : null,
                notify
            );

            log("Comment was created successfully!");
        } catch (Exception error) {
            log(error);
        }
     }

    private static Long getRoleNameId(String roleName) {

        // Глобальные проектные роли и их ID
        Map<String, Long> roleNameIds = [
            "Users":10000,
            "Administrators":10002,
            "Manager":10030,
            "QA":10120,
            "Developers":10001,
            "Readers":10080,
        ];


        // Если введенный roleName корректный -> возвращаем его ID
        // Если введенный roleName кривой -> возвращаем ID Readers
        return roleNameIds[roleName] ? roleNameIds[roleName] : 10080;
    }
    // ----------------------------------------------------------


    // ------------ "ПОЛУЧАЕМ ТИКЕТЫ ПО ФИЛЬТРУ" ----------------
    public static List<Issue> getIssues(String jql) {

        try {
            // Список задач, который мы будем отдавать из функции
            List<Issue> resultIssues = [];

            Integer start = 0;
            Integer get = 100;

            Object searchService = ComponentAccessor
                .getComponent(SearchService);

            Object parseResult =  searchService.parseQuery(user, jql);

            Boolean exist = true;
            while (exist) {

                Object pageFilter = new PagerFilter(start, get);
                Object searchResult = searchService
                    .search(
                        user,
                        parseResult.getQuery(),
                        pageFilter
                    );

                List<Issue> issues = searchResult
                    .results.collect {
                        getIssueObjById(it.id, false)
                    };

                if (!issues) { exist = false };
                resultIssues += issues;
                start += get;
            }

            if (resultIssues) {
                log("JQL Issues was obtained successfully!");
                return resultIssues;
            } else {
                log("JQL Issues not found :(");
                return null;
            }

        } catch (Exception error) {
            log(error);
            return null;
        }
    }
    //-----------------------------------------------------------


    // ---------------- "ПРОСТАВЛЯТЬ ЛЕЙБЛЫ" --------------------
    public static void addLabel(Issue issue, String labelName, Boolean notify=false) {

        try {
            // Получаем список текущих лейблов в задаче
            List<String> listOfLabels = getLabelsFromIssue(issue);

            // Докидываем наши лейблы и применяем изменения
            listOfLabels += labelName;
            labelManager.setLabels(user, issue.id, listOfLabels.toSet(), notify, false);
            log("Label was successfully add!");
        } catch (Exception error) {
            log(error);
        }
    }
    public static void addCloneLabel(Issue issue, String labelName) {

        try {
            HashSet<Label> labels = new HashSet<Label>()
            labels.addAll(issue.labels)
            labels.addAll(LabelParser.buildFromString(labelName))
            issue.setLabels(labels)

        } catch (Exception error) {
            log(error);
        }
    }

    private static List<String> getLabelsFromIssue(Issue issue) {

        // Получаем весь список лейблов из указанной задачи
        List<String> labels = labelManager.getLabels(issue.id).collect{it.getLabel()};

        return labels;
    }
    // ----------------------------------------------------------


    // -------------- "ПРОСТАВЛЯТЬ КОМПОНЕНТЫ" ------------------
    public static void addComponent(Issue issue, String componentName, Boolean notify=false) {

        try {
            // Переделываем задачу в изменяемую
            // Достаем из нее все компоненты
            // Получаем user'а под его имени
            // Получаем список доступных компонентов из проекта указанного тикета
            MutableIssue mutableIssue = issue;
            List<Object> components = mutableIssue.getComponents();
            Object component = getComponentFromProject(mutableIssue, componentName);


            // True -> добавляем наш компонент к текущему списку компонентов задачи, применяем изменения
            // False -> логируем инфу о том, что указанного компонента в проекте нет
            if (component) {
                components.push(component);
                mutableIssue.setComponent(components);
                issueManager.updateIssue(user, mutableIssue, EventDispatchOption.ISSUE_UPDATED, notify);
                log("Component was successfully add!");
            } else {
                log("Component $componentName doesn't exist!");
            }
        } catch (Exception error) {
            log(error);
        }
    }
    public static void addCloneComponent(Issue issue, String componentName) {

        try {
            // Переделываем задачу в изменяемую
            // Достаем из нее все компоненты
            // Получаем user'а под его имени
            // Получаем список доступных компонентов из проекта указанного тикета
            MutableIssue mutableIssue = issue;
            List<Object> components = mutableIssue.getComponents();
            Object component = getComponentFromProject(mutableIssue, componentName);


            // True -> добавляем наш компонент к текущему списку компонентов задачи, применяем изменения
            // False -> логируем инфу о том, что указанного компонента в проекте нет
            if (component) {
                components.push(component);
                mutableIssue.setComponent(components);
                log("Component was successfully add!");
            } else {
                log("Component $componentName doesn't exist!");
            }
        } catch (Exception error) {
            log(error);
        }
    }

    private static Object getComponentFromProject(MutableIssue mutableIssue, String componentName) {

        // Получаем объект проекта из указанного тикета
        Project project = mutableIssue.getProjectObject();

        // Проверяем есть ли указанный компонент в списке доступных компонентов проекта
        Object component = ComponentAccessor
            .getProjectComponentManager()
            .findByComponentName(project.getId(), componentName);

        return component;
    }
    // ----------------------------------------------------------


    // --------- "ДОБАВЛЕНИЕ/ИЗМЕНЕНИЕ КАСТОМНЫХ ПОЛЕЙ" ---------
    public static void setCf(Issue issue, String cfName, String cfValue, Boolean appendValue=true, Boolean notify=false) {

        try {
            MutableIssue mutableIssue = updateCf(
                issue,
                cfName,
                cfValue,
                appendValue,
                ""
            );

            if (mutableIssue) {
                issueManager.updateIssue(
                    user,
                    mutableIssue,
                    EventDispatchOption.DO_NOT_DISPATCH,
                    notify
                );

                log("$mutableIssue was successfully updated!")
            } 
        } catch (Exception error) {
            log(error);
        }
    }
    public static void setCf(Issue issue, String cfName, String cfValue, String sep, Boolean notify=false) {

        try {
            MutableIssue mutableIssue = updateCf(
                issue,
                cfName,
                cfValue,
                true,
                sep
            );

            if (mutableIssue) {
                issueManager.updateIssue(
                    user,
                    mutableIssue,
                    EventDispatchOption.DO_NOT_DISPATCH,
                    notify
                );

                log("$mutableIssue was successfully updated!")
            }
        } catch (Exception error) {
            log(error);
        }
    }
    public static void setCloneCf(Issue issue, String cfName, String cfValue, Boolean appendValue=true) {

        try {
            MutableIssue mutableIssue = updateCf(
                issue,
                cfName,
                cfValue,
                appendValue,
                ""
            );

            log("$mutableIssue was successfully updated!")
        } catch (Exception error) {
            log(error);
        }
    }
    public static void setCloneCf(Issue issue, String cfName, String cfValue, String sep) {

        try {
            MutableIssue mutableIssue = updateCf(
                issue,
                cfName,
                cfValue,
                true,
                sep
            );

            log("$mutableIssue was successfully updated!")
        } catch (Exception error) {
            log(error);
        }
    }
    public static void clearCf(Issue issue, String cfName, Boolean notify=false) {

        try {
            MutableIssue mutableIssue = updateCf(
                issue,
                cfName,
                null,
                false,
                ""
            );

            if (mutableIssue) {
                issueManager.updateIssue(
                    user,
                    mutableIssue,
                    EventDispatchOption.DO_NOT_DISPATCH,
                    notify
                );

                log("$mutableIssue was successfully updated!")
            }
        } catch (Exception error) {
            log(error);
        }
    }
    public static void clearCloneCf(Issue issue, String cfName) {

        try {
            MutableIssue mutableIssue = updateCf(
                issue,
                cfName,
                null,
                false,
                ""
            );

            log("$mutableIssue was successfully updated!")
        } catch (Exception error) {
            log(error);
        }
    }

    private static MutableIssue updateCf(Issue issue, String cfName, String cfValue, Boolean appendValue, String sep) {

        try {
            MutableIssue mutableIssue = issue;

            // issueCfValueList = [ значение_поля, объект_поля, категория ]
            List<Object> issueCfValueList = getCfValue(issue, cfName, false);

            if (!issueCfValueList) return null;

            switch(issueCfValueList[2]) {
                case "Group":
                    CustomField cf = issueCfValueList[1];
                    GroupManager groupManager = ComponentAccessor.getGroupManager();
                    Object group = groupManager.getGroup(cfValue);

                    mutableIssue.setCustomFieldValue(cf, group);
                break

                case "Group multiple":
                    CustomField cf = issueCfValueList[1];
                    GroupManager groupManager = ComponentAccessor.getGroupManager();
                    Object group = groupManager.getGroup(cfValue);

                    if (appendValue) {
                        List<Object> cfValueFromIssue = issueCfValueList[0];
                        List<Object> groupList = [group];
                        if (cfValueFromIssue) {
                            cfValueFromIssue.each {it ->
                                groupList.add(it);
                            }
                        }
                        mutableIssue.setCustomFieldValue(cf, groupList);
                    } else {
                        mutableIssue.setCustomFieldValue(cf, [group]);
                    }
                break

                case "List Options":
                    CustomField cf = issueCfValueList[1];
                    FieldConfig fieldConfig = cf.getRelevantConfig(issue);
                    Option value = optionsManager
                        .getOptions(fieldConfig)
                        ?.find { it.toString() == cfValue };

                    mutableIssue.setCustomFieldValue(cf, value);
                break

                case "List Options 2":
                    CustomField cf = issueCfValueList[1];
                    FieldConfig fieldConfig = cf.getRelevantConfig(issue);
                    Option value = optionsManager
                        .getOptions(fieldConfig)
                        ?.find { it.toString() == cfValue };

                    if (appendValue) {
                        List<String> cfValueFromIssue = issueCfValueList[0];
                        if (cfValueFromIssue) cfValueFromIssue.add(value);
                        else cfValueFromIssue = [value];
                        mutableIssue.setCustomFieldValue(cf, cfValueFromIssue);
                    } else {
                        mutableIssue.setCustomFieldValue(cf, [value]);
                    }
                break

                case "Single":
                    CustomField cf = issueCfValueList[1];
                    if (appendValue) {
                        String cfValueFromIssue = issueCfValueList[0];
                        if (cfValueFromIssue) cfValueFromIssue += sep+cfValue;
                        else cfValueFromIssue = cfValue;
                        mutableIssue.setCustomFieldValue(cf, cfValueFromIssue);
                    } else {
                        mutableIssue.setCustomFieldValue(cf, cfValue);
                    }
                break
                
                case "Date":
                    CustomField cf = issueCfValueList[1];
                    mutableIssue.setCustomFieldValue(cf, new Date().parse("yyyy-MM-dd H:m:s", cfValue).toTimestamp());
                break

                case "User List":
                    CustomField cf = issueCfValueList[1];

                    String username = "";
                    if (cfValue == "currentUser") {
                        username = currentUser.name
                    } else {
                        username = cfValue
                    }

                    ApplicationUser value = ComponentAccessor
                        .getUserUtil()
                        .getUser(username);

                    mutableIssue.setCustomFieldValue(cf, value);
                break

                case "Multiple Users List":
                    CustomField cf = issueCfValueList[1];

                    String username = "";
                    if (cfValue == "currentUser") {
                        username = currentUser.name
                    } else {
                        username = cfValue
                    }

                    ApplicationUser value = ComponentAccessor
                        .getUserUtil()
                        .getUser(username);

                    if (appendValue) {
                        List<ApplicationUser> cfValueFromIssue = issueCfValueList[0];
                        if (cfValueFromIssue) {
                            cfValueFromIssue.add(value);
                            mutableIssue.setCustomFieldValue(cf, cfValueFromIssue);
                        } else {
                            mutableIssue.setCustomFieldValue(cf, [value]);
                        }
                    } else {
                        mutableIssue.setCustomFieldValue(cf, [value]);
                    }
                break

                // case "Cascading List":
                //     log("This method not ready yet :(");

                //     CustomField cf = issueCfValueList[1];
                //     FieldConfig fieldConfig = cf.getRelevantConfig(issue);

                //     Options options = optionsManager.getOptions(fieldConfig);

                //     Option parentValue = options
                //         .find { it.value == cfValue };

                //     Option childValue = parentValue?.childOptions
                //         ?.find { it.value == cfValue };

                //     Map<String, Option> value = [null:parentValue, "1":childValue];

                //     cf.updateValue(
                //         null,
                //         mutableIssue,
                //         new ModifiedValue(issueCfValueList[0], value),
                //         new DefaultIssueChangeHolder()
                //     );
                // break

                default:
                    log("Can't find type for this field");
                    return null;
                break
            }

            return mutableIssue;

        } catch (Exception error) {
            log(error);
            return null;
        }
    }

    public static Object getCfValue(Issue issue, String cfName, Boolean raw=true) {

        try {
            // Получаем объект указанного Custom Field
            CustomField cfObject = getCfObject(cfName);

            // True:
            // * Либо возвращаем его "сырое" значение
            // * Либо возвращаем список с нужными данными для дальнейшей обработки ->
            //   -> cfValueWithCategory = [ значение_поля, объект_поля, категория ]
            //
            // False -> логируем инфу о том, что не можем найти поле по указанному названию
            if (cfObject) {
                // Получаем "сырое" значение из тикета
                Object rawCfValue = issue.getCustomFieldValue(cfObject);

                // Если нам не нужно обрабатывать значение, то можно вернуть обычный Object
                if (raw) return rawCfValue;

                // Получаем одну из 8 категорий
                // Создаем список:
                // [0] == "сырое" значение
                // [1] == объект поля
                // [2] == категория
                String cfTypeCategory = getCfTypeCategory(cfObject);

                if (cfTypeCategory) {
                    List<Object> cfValueWithCategory = [
                        rawCfValue,
                        cfObject,
                        cfTypeCategory
                    ];

                    return cfValueWithCategory;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception error) {
            log(error);
        }
    }
    public static Object getCfValue(Issue issue, List<String> cfName) {
        //TODO
        log("This method not ready yet :(");
        return null;
    }

    private static String getCfTypeCategory(CustomField cfOjbect) {

        // Названия типов полей на русском -> английская версия
        Map<String, String> typeLanguageMapping = [
            "Галочки":"Checkboxes",
            "Выпадающий список (одиночный выбор)":"Select List (single choice)",
            "Список выбора (множественный выбор)":"Select List (multiple choices)",
            "Список выбора (выпадающий)":"Select List (cascading)",
            "Текстовое Поле (однострочное)":"Text Field (single line)",
            "Текстовое Поле (многострочное)":"Text Field (multi-line)",
            "URL":"URL Field",
            "Переключатель":"Radio Buttons",
            "Выбор пользователя (один пользователь)":"User Picker (single user)",
            "Выбор пользователя (несколько пользователей)":"User Picker (multiple users)",
            "Выбор группы (одна групп)":"Group Picker (single groups)",
            "Выбор группы (несколько групп)":"Group Picker (multiple groups)",
            "Дата":"Date Picker",
            "Выбор даты и времени":"Date Time Picker",
            "Число (целое или дробное)":"Number field",
        ];

        // Сопоставление типов полей и их категорий
        Map<String, String> typeCategory = [
            // List with one option
            "Radio Buttons":"List Options",
            "Select List (single choice)":"List Options",

            // List with more than one option
            "Checkboxes":"List Options 2",
            "Select List (multiple choices)":"List Options 2",

            // Cascading list
            "Select List (cascading)":"Cascading List",

            // User List
            "User Picker (single user)":"User List",

            // Multiple Users List
            "Watcher Field":"Multiple Users List",
            "User Picker (multiple users)":"Multiple Users List",

            // Group
            "Group Picker (single group)":"Group",

            // Group multiple
            "Group Picker (multiple groups)":"Group multiple",

            // Single
            "Text Field (single line)":"Single",
            "Text Field (multi-line)":"Single",
            "URL Field":"Single",
            "Number Field":"Single",
            "Single Issue Picker":"Single",
            
            // Date
            "Date Picker":"Date",
            "Date Time Picker":"Date",
        ];

        try {
            // Получаем название типа поля на используемом языке пользователя
            // По умолчанию мы предпологаем, что язык все же будет английский
            String cfTypeName = cfOjbect.getCustomFieldType().name;
            Boolean langCheck = false;

            // Проверяем на наличие русских букв в полученном типе
            for (int i = 0; i < cfTypeName.length(); i++) {
                if (
                    Character.UnicodeBlock
                    .of(cfTypeName.charAt(i))
                    .equals(Character.UnicodeBlock.CYRILLIC)) {

                        langCheck = true;
                }
            }

            // True:
            // * Если все ок, получаем категорию поля и возвращаем значение
            // * Если не ок, логируем что категории для такого поля нет. Возвращаем null
            //
            // False:
            // * Получаем назваине типа поля на английском
            // * Потом все тоже самое, что и выше
            if (!langCheck) {
                String typeCategoryName = typeCategory[cfTypeName];

                if (typeCategoryName) {
                    return typeCategoryName;
                } else {
                    log("Can't find category for this customField: '$cfTypeName'");
                    return null;
                }
            } else {
                if (typeLanguageMapping[cfTypeName]) {
                    String typeName = typeLanguageMapping[cfTypeName];
                    String typeCategoryName = typeCategory[typeName];

                    if (typeCategoryName) {
                        return typeCategoryName;
                    } else {
                        log("Can't find category for this customField: '$typeName'");
                        return null;
                    }

                } else {
                    log("Can't find customField type for: '$cfTypeName'");
                    return null;
                }
            }
        } catch (Exception error) {
            log(error);
            return null;
        }
    }

    private static CustomField getCfObject(String cfName) {

        try {
            CustomFieldManager cfManager = ComponentAccessor
                .getCustomFieldManager();

            CustomField cf;

            // В зависимости от формата поля используем разные методы поиска
            if (cfName.startsWith("customfield_")) {
                cf = cfManager.getCustomFieldObject(cfName);
            } else {
                cf = cfManager.getCustomFieldObjectByName(cfName);
            }

            // True -> возвращаем объект поля
            // False -> логируем инфу о том, что мы не можем найти поле по указанному названию. озвращаем null
            if (cf) {
                return cf;
            } else {
                log("Can't find this customField: $cfName");
                log("Check if provided customField name is correct");
                return null;
            }

        } catch (Exception error) {
            log(error);
            return null;
        }
    }
    // ----------------------------------------------------------


    // ------------------- "ЛОГИРОВАНИЕ" ------------------------
    private static void log(Exception message) {

        // Полученный stack trace форматируем в читабельный формат
        logger.debug("[${this.getSimpleName()}] " + message);
        logger.debug("[${this.getSimpleName()}] " + message.getMessage().toString());
        logger.debug("[${this.getSimpleName()}] " + message
            .getStackTrace().toString()
            .replace(',',',\n')
            .replace('[','')
            .replace(']',''));
    }
    private static void log(String message) {

        // Логируем указанную строку
        logger.warn("[${this.getSimpleName()}] " + message);
    }
    // ----------------------------------------------------------
}

