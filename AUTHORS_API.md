# Author CRUD API - Testing Guide

## API Endpoints

The Author CRUD functionality provides the following endpoints under `/authors`:

### POST /authors
Create a new author (ADMIN only)
```json
{
  "name": "Chinua Achebe",
  "dateOfBirth": "1930-11-16",
  "biography": "Nigerian novelist and poet"
}
```

### PATCH /authors/{id}
Update an existing author (ADMIN only)
```json
{
  "name": "Updated Name",
  "dateOfBirth": "1930-11-16",
  "biography": "Updated biography"
}
```

### DELETE /authors/{id}
Delete an author (ADMIN only)
- Cannot delete if author has linked books

### GET /authors
Retrieve all authors (ADMIN and READER)

### GET /authors/{id}
Retrieve author by ID (ADMIN and READER)

## Security

- **ADMIN**: Full access (CREATE, READ, UPDATE, DELETE)
- **READER**: Read-only access (GET endpoints only)

## Business Rules

1. Author names must be unique
2. Date of birth cannot be in the future
3. Authors with linked books cannot be deleted
4. Biography has a maximum length of 10,000 characters

## OpenAPI Documentation

When the application is running, the API documentation is available at:
- Swagger UI: http://localhost:9090/swagger-ui.html
- OpenAPI JSON: http://localhost:9090/v3/api-docs

## Testing

Run the comprehensive test suite:
```bash
./mvnw test -Dtest="AuthorServiceImplTest,AuthorTests"
```

These tests cover:
- All CRUD operations
- Security validations
- Business rule enforcement
- Error handling