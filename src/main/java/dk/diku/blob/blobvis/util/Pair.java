package dk.diku.blob.blobvis.util;

public class Pair<T1, T2> {
	public Pair(T1 one, T2 two) {
		this.one = one;
		this.two = two;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((one == null) ? 0 : one.hashCode());
		result = prime * result + ((two == null) ? 0 : two.hashCode());
		return result;
	}
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj){
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Pair<T1,T2> other = (Pair<T1,T2>) obj;
		if (one == null) {
			if (other.one != null) {
				return false;
			}
		} else if (!one.equals(other.one)) {
			return false;
		}
		if (two == null) {
			if (other.two != null) {
				return false;
			}
		} else if (!two.equals(other.two)) {
			return false;
		}
		return true;
	}
	public T1 one;
	public T2 two;
	public T1 getOne() {
		return one;
	}
	public void setOne(T1 one) {
		this.one = one;
	}
	public T2 getTwo() {
		return two;
	}
	public void setTwo(T2 two) {
		this.two = two;
	}


}
