package app.db;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import app.util.EncryptionUtil;

import java.util.logging.Level;
import java.util.logging.Logger;

@Converter(autoApply = false) // Set to true if you want it to apply to all String fields automatically
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger LOGGER = Logger.getLogger(EncryptedStringConverter.class.getName());

    @Override
    public String convertToDatabaseColumn(String attribute) {
        try {
            return EncryptionUtil.encrypt(attribute);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error encrypting attribute.", e);
            // Depending on your policy, you might re-throw, return null, or log and proceed
            throw new RuntimeException("Could not encrypt data", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        try {
            return EncryptionUtil.decrypt(dbData);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error decrypting attribute.", e);
            // Depending on your policy, you might re-throw, return null, or log and proceed
            throw new RuntimeException("Could not decrypt data", e);
        }
    }
}