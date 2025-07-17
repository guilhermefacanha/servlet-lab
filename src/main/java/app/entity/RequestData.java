package app.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity
public class RequestData implements Serializable{
	
	private static final long serialVersionUID = 237733194593001583L;

	private static final String[] ignoreHeaders = {
			"Accept", "Accept-Encoding", "Accept-Language", "Cache-Control", "Connection", "Host",
			"Origin", "Referer", "User-Agent", "Sec-Fetch-Dest", "Sec-Fetch-Mode", "Sec-Fetch-Site",
			"JSESSIONID", "Cookie", "X-Requested-With", "Priority", "TE", "Upgrade-Insecure-Requests",
	};

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "requestdata_seq")
	@SequenceGenerator(name = "requestdata_seq", sequenceName = "requestdata_seq", allocationSize = 1)
	private Long id;

	private String type;
	private String ip;
	private String parameters;
	private Date date;
	private Date updateDate;

	@Column(length = 10000)
	private String payload;

	@Column(length = 10000)
	private String header;

	public String getHeaderListAsString() {
		if (getHeaderMap() == null || getHeaderMap().isEmpty()) {
			return "N/A";
		}
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> entry : getHeaderMap().entrySet()) {
			if (!StringUtils.equalsAnyIgnoreCase(entry.getKey(), ignoreHeaders)) {
				sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("<br/>");
			}
		}
		return sb.length() > 0 ? sb.toString() : "N/A";
	}

	// Utility methods (not persisted)
	@Transient
	public Map<String, String> getHeaderMap() {
		if (header == null) return new HashMap<>();
		try {
			return new ObjectMapper().readValue(header, new TypeReference<Map<String, String>>() {});
		} catch (Exception e) {
			return new HashMap<>();
		}
	}

	@Transient
	public void setHeaderMap(Map<String, String> map) {
		try {
			this.header = new ObjectMapper().writeValueAsString(map);
		} catch (Exception e) {
			this.header = "{}";
		}
	}
}
