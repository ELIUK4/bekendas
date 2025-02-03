# Galerija - Paveikslėlių Galerijos Aplikacija

Spring Boot aplikacija, leidžianti ieškoti ir valdyti paveikslėlius naudojant Pixabay API.

## Funkcionalumas

- Vartotojų autentifikacija (JWT)
- Paveikslėlių paieška per Pixabay API
- Mėgstamų paveikslėlių išsaugojimas
- Paieškos istorija
- Kategorijų valdymas

## Technologijos

- Java 17
- Spring Boot 3.2.2
- Spring Security su JWT
- MySQL duomenų bazė
- Maven

## Paleidimas

1. Sukurkite MySQL duomenų bazę pavadinimu `galerija`

2. Atnaujinkite `application.properties` failą su savo duomenų bazės prisijungimo duomenimis:
```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

3. Paleiskite aplikaciją:
```bash
mvn spring-boot:run
```

## Numatytieji Prisijungimo Duomenys

Admin vartotojas sukuriamas automatiškai pirmą kartą paleidus aplikaciją:
- Username: admin
- Password: admin123

## API Endpoints

### Autentifikacija
- POST `/api/auth/signin` - Prisijungimas
- POST `/api/auth/signup` - Registracija
- POST `/api/auth/signout` - Atsijungimas

### Paveikslėliai
- GET `/api/images/search` - Pixabay paveikslėlių paieška
- GET `/api/images/local` - Lokalių paveikslėlių paieška
- GET `/api/images/{id}` - Gauti konkretų paveikslėlį
- POST `/api/images` - Išsaugoti paveikslėlį

### Mėgstami
- POST `/api/favorites/{imageId}` - Pridėti į mėgstamus
- DELETE `/api/favorites/{imageId}` - Pašalinti iš mėgstamų
- GET `/api/favorites/check/{imageId}` - Patikrinti ar mėgstamas
- GET `/api/favorites` - Gauti visus mėgstamus

### Paieškos Istorija
- GET `/api/history` - Gauti paieškos istoriją
- DELETE `/api/history` - Išvalyti istoriją

### Kategorijos
- GET `/api/categories` - Gauti visas kategorijas
- GET `/api/categories/{name}` - Gauti kategoriją pagal pavadinimą
- POST `/api/categories/initialize` - Inicializuoti numatytąsias kategorijas (tik admin)
- POST `/api/categories` - Sukurti naują kategoriją (tik admin)
- DELETE `/api/categories/{id}` - Ištrinti kategoriją (tik admin)
