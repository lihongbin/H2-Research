/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.command.ddl;

import org.h2.command.CommandInterface;
import org.h2.constant.ErrorCode;
import org.h2.engine.Database;
import org.h2.engine.Session;
import org.h2.engine.UserDataType;
import org.h2.message.DbException;
import org.h2.table.Column;
import org.h2.table.Table;
import org.h2.value.DataType;

/**
 * This class represents the statement
 * CREATE DOMAIN
 */
//CREATE DOMAIN、CREATE TYPE、CREATE DATATYPE都是一样的
public class CreateUserDataType extends DefineCommand {

    private String typeName;
    private Column column;
    private boolean ifNotExists;

    public CreateUserDataType(Session session) {
        super(session);
    }

    public void setTypeName(String name) {
        this.typeName = name;
    }

    public void setColumn(Column column) {
        this.column = column;
    }

    public void setIfNotExists(boolean ifNotExists) {
        this.ifNotExists = ifNotExists;
    }

    public int update() {
        session.getUser().checkAdmin();
        session.commit(true);
        Database db = session.getDatabase();
        session.getUser().checkAdmin();
        if (db.findUserDataType(typeName) != null) {
            if (ifNotExists) {
                return 0;
            }
            throw DbException.get(ErrorCode.USER_DATA_TYPE_ALREADY_EXISTS_1, typeName);
        }
        DataType builtIn = DataType.getTypeByName(typeName);
        if (builtIn != null) {
            if (!builtIn.hidden) {
            	//从第二个名称开始的都是隐藏类型的，如下面的int
                //new String[]{"INTEGER", "INT", "MEDIUMINT", "INT4", "SIGNED"}
                //隐藏类型在用户在数据库中没有建表时可以覆盖
                //如CREATE DATATYPE IF NOT EXISTS int AS VARCHAR(255)
                //但是非隐藏类型就不能覆盖
                //如CREATE DATATYPE IF NOT EXISTS integer AS VARCHAR(255)
                throw DbException.get(ErrorCode.USER_DATA_TYPE_ALREADY_EXISTS_1, typeName);
            }
            
            //如果用户在数据库中没有建表，那么自定义的字段类型可以与内置字段类型的名字一样
            //如CREATE DATATYPE IF NOT EXISTS int AS VARCHAR(255)
            Table table = session.getDatabase().getFirstUserTable();
            if (table != null) {
                throw DbException.get(ErrorCode.USER_DATA_TYPE_ALREADY_EXISTS_1, typeName + " (" + table.getSQL() + ")");
            }
        }
        int id = getObjectId();
        UserDataType type = new UserDataType(db, id, typeName);
        type.setColumn(column);
        db.addDatabaseObject(session, type);
        return 0;
    }

    public int getType() {
        return CommandInterface.CREATE_DOMAIN;
    }

}
