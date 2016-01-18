package edu.mayo.bmi.medtagger.ml.feature;

import java.io.Serializable;



public class Feature implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -3215288856677656204L;

  protected String name;
  protected Object value;
  protected double weight;

  public Feature() {
  }

  public Feature(Object value) {
    this.value = value;
  }

  // Default to existence
  public Feature(String name, Object value) {
    this.name = name;
    this.value = value;
    this.weight = 1;
  }

  public Feature(String name, Object value, double weight) {
	    this.name = name;
	    this.value = value;
	    this.weight = weight;
	  }

  public static Feature createFeature(String namePrefix, Feature feature) {
    return new Feature(createName(namePrefix, feature.name), feature.value);
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public void setWeight(int count) {
	    this.weight = count;
	  }
  public double getWeight() {
	    return weight;
	  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public static String createName(String... names) {
    StringBuffer buffer = new StringBuffer();
    for (String name : names) {
      if (name != null) {
        buffer.append(name);
        buffer.append('_');
      }
    }
    if (buffer.length() > 0) {
      buffer.deleteCharAt(buffer.length() - 1);
    }
    return buffer.toString();
  }

  public String toString() {
    String className = Feature.class.getSimpleName();
    return String.format("%s(<%s>, <%s>)", className, this.name, this.value);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Feature) {
      Feature other = (Feature) obj;
      boolean nameMatch = (this.name == null && other.name == null)
          || (this.name != null && this.name.equals(other.name));
      boolean valueMatch = (this.value == null && other.value == null)
          || (this.value != null && this.value.equals(other.value));
      return nameMatch && valueMatch;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int hash = 1;
    hash = hash * 31 + (this.name == null ? 0 : this.name.hashCode());
    hash = hash * 31 + (this.value == null ? 0 : this.value.hashCode());
    return hash;
  }

}
