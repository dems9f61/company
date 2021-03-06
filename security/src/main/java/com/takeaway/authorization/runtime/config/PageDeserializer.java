package com.takeaway.authorization.runtime.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: StMinko Date: 15.01.2020 Time: 10:45
 * <p>
 */
class PageDeserializer extends JsonDeserializer<Page<?>> implements ContextualDeserializer
{
    private static final String CONTENT        = "content";

    private static final String NUMBER         = "number";

    private static final String SIZE           = "size";

    private static final String TOTAL_ELEMENTS = "totalElements";

    private JavaType valueType;

    @Override
    public Page<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException
    {
        final CollectionType valuesListType = deserializationContext.getTypeFactory()
                                                                    .constructCollectionType(List.class, valueType);

        List<?> list = new ArrayList<>();
        int pageNumber = 0;
        int pageSize = 0;
        long total = 0;
        if (jsonParser.isExpectedStartObjectToken())
        {
            jsonParser.nextToken();
            if (jsonParser.hasTokenId(JsonTokenId.ID_FIELD_NAME))
            {
                String propName = jsonParser.getCurrentName();
                do
                {
                    jsonParser.nextToken();
                    switch (propName)
                    {
                        case CONTENT:
                            list = deserializationContext.readValue(jsonParser, valuesListType);
                            break;
                        case NUMBER:
                            pageNumber = deserializationContext.readValue(jsonParser, Integer.class);
                            break;
                        case SIZE:
                            pageSize = deserializationContext.readValue(jsonParser, Integer.class);
                            break;
                        case TOTAL_ELEMENTS:
                            total = deserializationContext.readValue(jsonParser, Long.class);
                            break;
                        default:
                            jsonParser.skipChildren();
                            break;
                    }
                }
                while (((propName = jsonParser.nextFieldName())) != null);
            }
            else
            {
                deserializationContext.handleUnexpectedToken(handledType(), jsonParser);
            }
        }
        else
        {
            deserializationContext.handleUnexpectedToken(handledType(), jsonParser);
        }

        // Note that Sort field of Page is ignored here.
        // Feel free to add more switch cases above to deserialize it as well.
        return new PageImpl<>(list, PageRequest.of(pageNumber, pageSize), total);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property)
    {
        // This is the Page actually
        final JavaType wrapperType = ctxt.getContextualType();
        final PageDeserializer deserializer = new PageDeserializer();
        // This is the parameter of Page
        deserializer.valueType = wrapperType.containedType(0);
        return deserializer;
    }
}
