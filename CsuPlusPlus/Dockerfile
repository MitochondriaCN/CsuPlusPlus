FROM mcr.microsoft.com/dotnet/aspnet:11.0-preview AS base
USER $APP_UID
WORKDIR /app
EXPOSE 8080
EXPOSE 8081

FROM mcr.microsoft.com/dotnet/sdk:11.0-preview AS build
ARG BUILD_CONFIGURATION=Release
WORKDIR /src
COPY ["CsuPlusPlus/CsuPlusPlus.csproj", "CsuPlusPlus/"]
RUN dotnet restore "CsuPlusPlus/CsuPlusPlus.csproj"
COPY . .
WORKDIR "/src/CsuPlusPlus"
RUN dotnet build "./CsuPlusPlus.csproj" -c $BUILD_CONFIGURATION -o /app/build

FROM build AS publish
ARG BUILD_CONFIGURATION=Release
RUN dotnet publish "./CsuPlusPlus.csproj" -c $BUILD_CONFIGURATION -o /app/publish /p:UseAppHost=false

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "CsuPlusPlus.dll"]
