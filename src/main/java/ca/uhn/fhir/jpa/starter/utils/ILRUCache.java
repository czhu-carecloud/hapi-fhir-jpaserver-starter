package ca.uhn.fhir.jpa.starter.utils;

public interface ILRUCache<K, V> {
  public void put(K key, V value);
  public V get(K key);
}
