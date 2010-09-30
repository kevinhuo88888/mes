package com.qcadoo.mes.model.search.restrictions.internal;

import com.qcadoo.mes.model.search.Restriction;

public abstract class BaseRestriction implements Restriction, HibernateRestriction {

    private final String fieldName;

    private final Object value;

    public BaseRestriction(final String fieldName, final Object value) {
        this.fieldName = fieldName;
        this.value = value;
    }

    @Override
    public final String getFieldName() {
        return fieldName;
    }

    @Override
    public final Object getValue() {
        return value;
    }

}
