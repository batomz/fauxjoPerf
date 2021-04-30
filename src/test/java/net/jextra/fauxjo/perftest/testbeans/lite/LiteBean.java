package net.jextra.fauxjo.perftest.testbeans.lite;

import net.jextra.fauxjo.bean.Fauxjo;
import net.jextra.fauxjo.bean.FauxjoField;
import net.jextra.fauxjo.bean.FauxjoPrimaryKey;

import java.math.BigDecimal;

/** * Represents a minimal bean which is the most conservative performance test due to having very few fields. */
public class LiteBean extends Fauxjo implements java.io.Serializable {
	static final long serialVersionUID = 42L;

	@FauxjoPrimaryKey
	@FauxjoField("id")
	private Integer id;

	@FauxjoField("name")
	private String name;

	@FauxjoField("weight")
	private BigDecimal weight;

	@FauxjoField("age")
	private Integer age;

	public LiteBean() {
		id = 1;
		name = "Bob";
	}

	public Integer getId() {
		return id;
	}
	public void setId(Integer _id) {
		id = _id;
	}
	public String getName() {
		return name;
	}
	public void setName(String _name) {
		name = _name;
	}
	public BigDecimal getWeight() {
		return weight;
	}
	public void setWeight(BigDecimal _weight) {
		weight = _weight;
	}
	public Integer getAge() {
		return age;
	}
	public void setAge(Integer _age) {
		age = _age;
	}

	/** * Return a terse description. */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
			"id=" + id +
			", name='" + name + '\'' +
			", weight=" + weight +
			", age=" + age +
			'}';
	}
}


