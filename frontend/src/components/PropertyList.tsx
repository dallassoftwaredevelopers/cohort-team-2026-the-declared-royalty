import { useEffect, useState } from "react";
import { apiClient } from "../app/services/apiClient";
import type { PropertyResponse } from "../types/property";

function PropertyList() {
  const [properties, setProperties] = useState<PropertyResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    apiClient
      .get<PropertyResponse[]>("/api/v1/properties", undefined, false)
      .then((result) => {
        if (active) setProperties(result);
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : "Unable to load properties.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <section className="mx-auto max-w-3xl p-6">
      <h1 className="mb-4 text-2xl font-semibold">Properties</h1>

      {loading && <p>Loading properties...</p>}
      {error && <p role="alert">Could not load properties: {error}</p>}
      {!loading && !error && properties.length === 0 && (
        <p>No properties found.</p>
      )}

      {!loading && !error && properties.length > 0 && (
        <ul className="divide-y divide-gray-200">
          {properties.map((property) => (
            <li className="py-4" key={property.propertyId}>
              <h2 className="font-medium">{property.name}</h2>
              <p>{property.streetAddress}</p>
              <p>
                {property.city}, {property.state} {property.zipCode}
              </p>
              <p>{property.status}</p>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

export default PropertyList;
