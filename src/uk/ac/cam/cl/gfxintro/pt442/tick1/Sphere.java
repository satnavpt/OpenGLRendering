package uk.ac.cam.cl.gfxintro.pt442.tick1;

import java.util.ArrayList;
import java.util.List;

public class Sphere extends SceneObject {

	// Sphere coefficients
	private final double SPHERE_KD = 0.8;
	private final double SPHERE_KS = 1.2;
	private final double SPHERE_ALPHA = 10;
	private final double SPHERE_REFLECTIVITY = 0.3;

	// The world-space position of the sphere
	private Vector3 position;

	public Vector3 getPosition() {
		return position;
	}

	// The radius of the sphere in world units
	private double radius;

	public Sphere(Vector3 position, double radius, ColorRGB colour) {
		this.position = position;
		this.radius = radius;
		this.colour = colour;

		this.phong_kD = SPHERE_KD;
		this.phong_kS = SPHERE_KS;
		this.phong_alpha = SPHERE_ALPHA;
		this.reflectivity = SPHERE_REFLECTIVITY;
	}

	public Sphere(Vector3 position, double radius, ColorRGB colour, double kD, double kS, double alphaS, double reflectivity) {
		this.position = position;
		this.radius = radius;
		this.colour = colour;

		this.phong_kD = kD;
		this.phong_kS = kS;
		this.phong_alpha = alphaS;
		this.reflectivity = reflectivity;
	}

	/*
	 * Calculate intersection of the sphere with the ray. If the ray starts inside the sphere,
	 * intersection with the surface is also found.
	 */
	public RaycastHit intersectionWith(Ray ray) {

		// Get ray parameters
		Vector3 O = ray.getOrigin();
		Vector3 D = ray.getDirection();

		// Get sphere parameters
		Vector3 C = position;
		double r = radius;

		// Calculate quadratic coefficients
		double a = D.dot(D);
		double b = 2 * D.dot(O.subtract(C));
		double c = (O.subtract(C)).dot(O.subtract(C)) - Math.pow(r, 2);

		RaycastHit empty = new RaycastHit();

		double d2 = ((Math.pow(b, 2))-(4)*(a)*(c));
		if (d2 < 0) {
			return empty;
		} else if (d2 == 0) {
			double d = Math.sqrt(d2);
			double s = ((-b) + d) / (2 * a);
			if (s < 0) {
				return empty;
			} else {
				Vector3 p = O.add(D.scale(s));
				double distOfP = p.subtract(O).magnitude();
				return new RaycastHit(this, distOfP, p, getNormalAt(p));
			}
		} else {
			double d = Math.sqrt(d2);
			double s1 = ((-b) + d) / (2 * a);
			double s2 = ((-b) - d) / (2 * a);
			if (s1 >= 0 && s2 >= 0) {
				Vector3 p1 = O.add(D.scale(s1));
				double distOfP1 = p1.subtract(O).magnitude();
				Vector3 p2 = O.add(D.scale(s2));
				double distOfP2 = p2.subtract(O).magnitude();
				if (distOfP1 < distOfP2) {
					return new RaycastHit(this, distOfP1, p1, getNormalAt(p1));
				} else {
					return new RaycastHit(this, distOfP2, p2, getNormalAt(p2));
				}
			} else if (s1 >= 0 && s2 < 0) {
				Vector3 p1 = O.add(D.scale(s1));
				double distOfP1 = p1.subtract(O).magnitude();
				return new RaycastHit(this, distOfP1, p1, getNormalAt(p1));
			} else if (s2 >= 0 && s1 < 0) {
				Vector3 p2 = O.add(D.scale(s2));
				double distOfP2 = p2.subtract(O).magnitude();
				return new RaycastHit(this, distOfP2, p2, getNormalAt(p2));
			} else {
				return empty;
			}
		}
	}

	// Get normal to surface at position
	public Vector3 getNormalAt(Vector3 position) {
		return position.subtract(this.position).normalised();
	}
}
