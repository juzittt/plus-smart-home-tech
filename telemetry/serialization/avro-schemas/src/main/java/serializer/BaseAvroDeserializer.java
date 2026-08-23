package serializer;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HexFormat;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private static final Logger log = LoggerFactory.getLogger(BaseAvroDeserializer.class);
    private static final HexFormat hexFormat = HexFormat.ofDelimiter(":");

    private final DecoderFactory decoderFactory;
    private final Schema schema;
    private final SpecificDatumReader<T> reader;

    public BaseAvroDeserializer (Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.schema = schema;
        this.reader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            log.debug("Deserializing message from topic '{}', size {} bytes", topic, data.length);
            log.trace("Raw bytes: {}", hexFormat.formatHex(data));

            BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);

            T result = reader.read(null, decoder);

            log.debug("Successfully deserialized message from topic '{}': {}", topic, result.getClass().getSimpleName());
            return result;
        } catch (IOException e) {
            throw new SerializationException("Error deserializing message from topic: " + topic, e);
        }
    }
}
