# syntax=docker/dockerfile:1

FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src

COPY global.json ./
COPY Directory.Build.props ./
COPY MbtiEnterpriseSimilarity.sln ./
COPY src/MbtiEnterpriseSimilarity.App/MbtiEnterpriseSimilarity.App.csproj src/MbtiEnterpriseSimilarity.App/
COPY tests/MbtiEnterpriseSimilarity.Tests/MbtiEnterpriseSimilarity.Tests.csproj tests/MbtiEnterpriseSimilarity.Tests/
RUN dotnet restore MbtiEnterpriseSimilarity.sln

COPY . .
RUN dotnet publish src/MbtiEnterpriseSimilarity.App/MbtiEnterpriseSimilarity.App.csproj \
    -c Release \
    -o /app/publish \
    --no-restore

FROM mcr.microsoft.com/dotnet/runtime:9.0 AS runtime
WORKDIR /app

RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

COPY --from=build /app/publish ./
COPY data ./data
RUN mkdir -p /app/reports && chown -R appuser:appgroup /app

USER appuser

ENTRYPOINT ["dotnet", "MbtiEnterpriseSimilarity.App.dll"]
CMD [
  "--input", "/app/data/CSS121_MBTI_2026_68.csv",
  "--target-id", "68090500418",
  "--top", "5",
  "--max-skipped-ratio", "0.2",
  "--exclude-id", "99999999999",
  "--mode", "zscore",
  "--output-dir", "/app/reports"
]
